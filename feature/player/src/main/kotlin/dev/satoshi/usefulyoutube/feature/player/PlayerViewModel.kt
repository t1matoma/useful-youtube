package dev.satoshi.usefulyoutube.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.AddChannelUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveChannelsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoWithChaptersUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.Chapter
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Plays one video or, with a playlist id, a playlist: the current video changes inside the screen, so
 * "back" returns to the playlist. The current video is kept in the saved state. A video continues where it
 * stopped last time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val observeVideo: ObserveVideoWithChaptersUseCase,
        observeChannels: ObserveChannelsUseCase,
        observePlaylist: ObservePlaylistUseCase,
        private val addChannel: AddChannelUseCase,
        private val playbackTracker: PlaybackTracker,
    ) : ViewModel() {
        private data class ScreenState(
            val isDescriptionExpanded: Boolean = false,
            val isSubscribing: Boolean = false,
            val subscribeError: ChannelError? = null,
            /** `null` until the player reports a position or the user picks a chapter. */
            val currentChapterIndex: Int? = null,
        )

        private data class LoadedVideo(
            val video: Video?,
            val startPosition: Duration,
            val chapters: List<Chapter>,
        )

        private val currentVideoId =
            savedStateHandle.getStateFlow(
                CURRENT_VIDEO_KEY,
                checkNotNull(savedStateHandle.get<String>(PlayerDestination.VIDEO_ID_ARGUMENT)),
            )

        /** Shared by the UI state and the queue events; `null` for a single video. */
        private val playlist: StateFlow<PlaylistWithVideos?> =
            (
                savedStateHandle
                    .get<Long>(PlayerDestination.PLAYLIST_ID_ARGUMENT)
                    ?.let { observePlaylist(PlaylistId(it)) }
                    ?: flowOf(null)
            ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
        private val screenState = MutableStateFlow(ScreenState())

        val uiState: StateFlow<PlayerUiState> =
            combine(
                currentVideoId.flatMapLatest { id -> observeVideoWithStart(VideoId(id)) },
                observeChannels(),
                playlist,
                screenState,
            ) { loaded, channels, playlist, screen ->
                val video = loaded.video
                if (video == null) {
                    PlayerUiState.NotFound
                } else {
                    PlayerUiState.Content(
                        video = video,
                        startPosition = loaded.startPosition,
                        isDescriptionExpanded = screen.isDescriptionExpanded,
                        isChannelSubscribed = channels.any { it.id == video.channelId },
                        isSubscribing = screen.isSubscribing,
                        subscribeError = screen.subscribeError,
                        queue = playlist?.queueAt(video.id),
                        chapters = loaded.chapters,
                        currentChapterIndex =
                            screen.currentChapterIndex ?: loaded.chapters.indexAt(loaded.startPosition),
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlayerUiState.Loading)

        fun onEvent(event: PlayerUiEvent) {
            when (event) {
                PlayerUiEvent.DescriptionToggled -> {
                    screenState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
                }

                PlayerUiEvent.SubscribeToChannel -> {
                    subscribe()
                }

                PlayerUiEvent.PlaybackStarted -> {
                    currentVideo()?.let { video -> viewModelScope.launch { playbackTracker.onStarted(video) } }
                }

                is PlayerUiEvent.PlaybackProgressed -> {
                    val content = currentContent()?.takeIf { it.video.id == event.videoId } ?: return
                    screenState.update {
                        it.copy(currentChapterIndex = content.chapters.indexAt(event.position.position))
                    }
                    viewModelScope.launch { playbackTracker.onProgress(content.video, event.position) }
                }

                is PlayerUiEvent.ChapterSelected -> {
                    if (event.index in currentContent()?.chapters.orEmpty().indices) {
                        screenState.update { it.copy(currentChapterIndex = event.index) }
                    }
                }

                PlayerUiEvent.PlaybackStopped -> {
                    viewModelScope.launch { playbackTracker.onStopped() }
                }

                PlayerUiEvent.PreviousClicked -> {
                    viewModelScope.launch { playbackTracker.onStopped() }
                    moveBy(-1)
                }

                PlayerUiEvent.NextClicked -> {
                    viewModelScope.launch { playbackTracker.onStopped() }
                    moveBy(1)
                }

                PlayerUiEvent.VideoEnded -> {
                    viewModelScope.launch { playbackTracker.onEnded() }
                    moveBy(1)
                }
            }
        }

        private fun moveBy(offset: Int) {
            val videos = playlist.value?.videos ?: return
            val index = videos.indexOfFirst { it.id.value == currentVideoId.value }
            if (index < 0) return
            val next = videos.getOrNull(index + offset)?.id ?: return
            screenState.value = ScreenState()
            savedStateHandle[CURRENT_VIDEO_KEY] = next.value
        }

        /** The start position is read once, so saving the position while the video plays does not reload it. */
        private fun observeVideoWithStart(id: VideoId): Flow<LoadedVideo> =
            flow {
                val startPosition = playbackTracker.startPosition(id)
                emitAll(
                    observeVideo(id).map { loaded ->
                        LoadedVideo(loaded?.video, startPosition, loaded?.chapters.orEmpty())
                    },
                )
            }

        private fun currentContent() = uiState.value as? PlayerUiState.Content

        private fun currentVideo() = currentContent()?.video

        /** The chapter that contains [position]; `null` without chapters. */
        private fun List<Chapter>.indexAt(position: Duration): Int? =
            indexOfLast { it.start <= position }.takeIf { it >= 0 }

        /** The channel id is verified through its RSS feed, which costs no Data API quota. */
        private fun subscribe() {
            val content = uiState.value as? PlayerUiState.Content ?: return
            if (content.isSubscribing || content.isChannelSubscribed) return
            screenState.update { it.copy(isSubscribing = true, subscribeError = null) }
            viewModelScope.launch {
                val result = addChannel(content.video.channelId.value)
                val error =
                    (result as? AppResult.Failure)?.error?.takeUnless { it == ChannelError.AlreadySubscribed }
                screenState.update { it.copy(isSubscribing = false, subscribeError = error) }
            }
        }

        /** `null` when the video is not in the playlist anymore, for example after it was removed. */
        private fun PlaylistWithVideos.queueAt(videoId: VideoId): PlaybackQueue? {
            val index = videos.indexOfFirst { it.id == videoId }
            return if (index < 0) null else PlaybackQueue(playlist.title, position = index + 1, size = videos.size)
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val CURRENT_VIDEO_KEY = "currentVideoId"
        }
    }
