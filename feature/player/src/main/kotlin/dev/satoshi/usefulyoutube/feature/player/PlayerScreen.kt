package dev.satoshi.usefulyoutube.feature.player

import android.app.Activity
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.error.message
import dev.satoshi.usefulyoutube.core.ui.video.relativeDate
import dev.satoshi.usefulyoutube.feature.player.component.ChapterList
import dev.satoshi.usefulyoutube.feature.player.component.FullscreenPlayer
import dev.satoshi.usefulyoutube.feature.player.component.FullscreenVideo
import dev.satoshi.usefulyoutube.feature.player.component.PictureInPictureEffect
import dev.satoshi.usefulyoutube.feature.player.component.PlayerController
import dev.satoshi.usefulyoutube.feature.player.component.QueueButton
import dev.satoshi.usefulyoutube.feature.player.component.QueueControls
import dev.satoshi.usefulyoutube.feature.player.component.YouTubePlayer
import dev.satoshi.usefulyoutube.feature.player.component.rememberIsInPictureInPictureMode
import dev.satoshi.usefulyoutube.feature.player.rating.RatingButtons
import dev.satoshi.usefulyoutube.feature.player.rating.RatingUiEvent
import dev.satoshi.usefulyoutube.feature.player.rating.RatingUiState
import dev.satoshi.usefulyoutube.feature.player.rating.RatingViewModel
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistSheet
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistUiEvent
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistUiState
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistViewModel
import kotlin.time.Duration

private const val COLLAPSED_DESCRIPTION_LINES = 3
private val ContentPadding = 20.dp
private val SectionSpacing = 18.dp
private val ItemSpacing = 12.dp
private val SmallSpacing = 2.dp
private val ButtonIconSize = 18.dp
private val ButtonIconSpacing = 8.dp
private val CardPadding = 16.dp
private val HeaderPadding = 4.dp

@Composable
fun PlayerRoute(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    saveViewModel: SaveToPlaylistViewModel = hiltViewModel(),
    ratingViewModel: RatingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by saveViewModel.uiState.collectAsStateWithLifecycle()
    val ratingState by ratingViewModel.uiState.collectAsStateWithLifecycle()
    // The sheet and the rating belong to the video that is playing now, which changes while a playlist plays.
    (state as? PlayerUiState.Content)?.video?.id?.let { videoId ->
        LaunchedEffect(videoId) {
            saveViewModel.onEvent(SaveToPlaylistUiEvent.VideoChanged(videoId))
            ratingViewModel.onEvent(RatingUiEvent.VideoChanged(videoId))
        }
    }
    PlayerScreen(
        state = state,
        onEvent = viewModel::onEvent,
        saveState = saveState,
        onSaveEvent = saveViewModel::onEvent,
        ratingState = ratingState,
        onRatingEvent = ratingViewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onEvent: (PlayerUiEvent) -> Unit,
    saveState: SaveToPlaylistUiState,
    onSaveEvent: (SaveToPlaylistUiEvent) -> Unit,
    ratingState: RatingUiState,
    onRatingEvent: (RatingUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val consentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            onRatingEvent(RatingUiEvent.ConsentFinished(granted = result.resultCode == Activity.RESULT_OK))
        }
    ratingState.consentScreen?.let { consentScreen ->
        LaunchedEffect(consentScreen) {
            consentLauncher.launch(IntentSenderRequest.Builder(consentScreen).build())
            onRatingEvent(RatingUiEvent.ConsentLaunched)
        }
    }
    // The fullscreen view belongs to the embedded player, so it is plain UI state and not part of the UiState.
    var fullscreenVideo by remember { mutableStateOf<FullscreenVideo?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var videoBounds by remember { mutableStateOf<Rect?>(null) }
    val playerController = remember { PlayerController() }
    val isInPictureInPicture = rememberIsInPictureInPictureMode()
    if (state is PlayerUiState.Content) {
        PictureInPictureEffect(
            isPlaying = isPlaying,
            videoBounds = videoBounds,
            onPlay = playerController::play,
            onPause = playerController::pause,
        )
    }
    val background = if (isInPictureInPicture) Color.Black else MaterialTheme.colorScheme.background
    Box(modifier = modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The picture-in-picture window shows only the video.
            if (!isInPictureInPicture) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.padding(HeaderPadding)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_player_back),
                    )
                }
            }
            val contentModifier = Modifier.fillMaxWidth().weight(1f)
            when (state) {
                PlayerUiState.Loading -> {
                    LoadingContent(contentModifier)
                }

                PlayerUiState.NotFound -> {
                    MessageContent(
                        message = stringResource(R.string.feature_player_not_found),
                        modifier = contentModifier,
                    )
                }

                is PlayerUiState.Content -> {
                    // Leaving the screen saves where the video stopped.
                    DisposableEffect(Unit) { onDispose { onEvent(PlayerUiEvent.PlaybackStopped) } }
                    Column(modifier = contentModifier, verticalArrangement = Arrangement.Center) {
                        YouTubePlayer(
                            videoId = state.video.id,
                            startPosition = state.startPosition,
                            onPlaybackStarted = { onEvent(PlayerUiEvent.PlaybackStarted) },
                            onProgress = { id, position -> onEvent(PlayerUiEvent.PlaybackProgressed(id, position)) },
                            onPaused = { onEvent(PlayerUiEvent.PlaybackStopped) },
                            onVideoEnded = { onEvent(PlayerUiEvent.VideoEnded) },
                            onPlayingChange = { isPlaying = it },
                            onEnterFullscreen = { fullscreenVideo = it },
                            onExitFullscreen = { fullscreenVideo = null },
                            modifier =
                                Modifier.fillMaxWidth().onGloballyPositioned {
                                    videoBounds = Rect().apply { it.boundsInWindow().toAndroidRectF().roundOut(this) }
                                },
                            controller = playerController,
                        )
                        if (!isInPictureInPicture) {
                            VideoDetails(
                                state = state,
                                onEvent = onEvent,
                                actions = { VideoActions(ratingState, onRatingEvent, onSaveEvent) },
                                onSeek = playerController::seekTo,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        fullscreenVideo?.let { video ->
            // Previous and next are shown while the video is paused, together with the player's own controls.
            val queue = (state as? PlayerUiState.Content)?.queue?.takeUnless { isPlaying || isInPictureInPicture }
            FullscreenPlayer(
                video = video,
                sideControls =
                    queue?.let {
                        {
                            QueueButton(next = false, queue = it, onEvent = onEvent)
                            QueueButton(next = true, queue = it, onEvent = onEvent)
                        }
                    },
            )
        }
    }
    if (!isInPictureInPicture) {
        SaveToPlaylistSheet(saveState, onSaveEvent)
    }
}

@Composable
private fun VideoDetails(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
    actions: @Composable () -> Unit,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The chapters scroll sideways to the screen edges, so the sections are padded one by one.
    val horizontalPadding = Modifier.padding(horizontal = ContentPadding)
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(vertical = ContentPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        Text(text = state.video.title, style = MaterialTheme.typography.titleLarge, modifier = horizontalPadding)
        ChannelRow(state, onEvent, modifier = horizontalPadding)
        state.subscribeError?.let { error ->
            Text(error.message(), color = MaterialTheme.colorScheme.error, modifier = horizontalPadding)
        }
        Column(modifier = horizontalPadding, verticalArrangement = Arrangement.spacedBy(ItemSpacing)) { actions() }
        state.queue?.let { queue -> QueueControls(queue, onEvent, modifier = horizontalPadding) }
        if (state.chapters.isNotEmpty()) {
            ChapterList(
                chapters = state.chapters,
                currentIndex = state.currentChapterIndex,
                onChapterClick = { index, chapter ->
                    onSeek(chapter.start)
                    onEvent(PlayerUiEvent.ChapterSelected(index))
                },
                contentPadding = PaddingValues(horizontal = ContentPadding),
            )
        }
        if (state.video.description.isNotBlank()) {
            DescriptionCard(state, onEvent, modifier = horizontalPadding)
        }
    }
}

/** Like, dislike and "Save to playlist", with the reason under them when YouTube did not accept a rating. */
@Composable
private fun VideoActions(
    ratingState: RatingUiState,
    onRatingEvent: (RatingUiEvent) -> Unit,
    onSaveEvent: (SaveToPlaylistUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ItemSpacing), verticalAlignment = Alignment.CenterVertically) {
        RatingButtons(ratingState, onRatingEvent)
        OutlinedButton(onClick = { onSaveEvent(SaveToPlaylistUiEvent.SaveClicked) }) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Text(stringResource(R.string.feature_player_save), modifier = Modifier.padding(start = ButtonIconSpacing))
        }
    }
    ratingState.error?.let { error ->
        Text(error.message(), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ChannelRow(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(ItemSpacing)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SmallSpacing)) {
            Text(
                text = state.video.channelTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = relativeDate(state.video),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.isChannelSubscribed) {
            val colors = MaterialTheme.colorScheme
            Button(
                onClick = { onEvent(PlayerUiEvent.SubscribeToChannel) },
                enabled = !state.isSubscribing,
                colors = ButtonDefaults.buttonColors(containerColor = colors.onSurface, contentColor = colors.surface),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
                Text(
                    stringResource(R.string.feature_player_subscribe),
                    modifier = Modifier.padding(start = ButtonIconSpacing),
                )
            }
        }
    }
}

@Composable
private fun DescriptionCard(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded = state.isDescriptionExpanded
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(start = CardPadding, top = CardPadding, end = CardPadding)) {
            SelectionContainer {
                Text(
                    text = state.video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { onEvent(PlayerUiEvent.DescriptionToggled) }) {
                Text(
                    stringResource(
                        if (isExpanded) R.string.feature_player_show_less else R.string.feature_player_show_more,
                    ),
                )
            }
        }
    }
}
