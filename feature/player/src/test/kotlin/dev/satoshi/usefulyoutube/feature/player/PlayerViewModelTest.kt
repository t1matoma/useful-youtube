package dev.satoshi.usefulyoutube.feature.player

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelInputParser
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.domain.usecase.AddChannelUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.GetResumePositionUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveChannelsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoWithChaptersUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ParseChaptersUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RecordWatchUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SavePlaybackPositionUseCase
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.Chapter
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeRecommendationRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val video = TestData.video(id = "abc")
    private val otherChannel = Channel(ChannelId("UCcccccccccccccccccccccc"), "Other channel")
    private val foundVideo = TestData.video(id = "found", channel = otherChannel)
    private val channelRepository = FakeChannelRepository(listOf(TestData.firstChannel))
    private val playlistRepository = FakePlaylistRepository()
    private var historyRepository = FakeHistoryRepository()
    private val watchedAt = Instant.parse("2026-11-07T10:00:00Z")
    private val chapteredVideo =
        TestData
            .video(id = "chaptered", channel = otherChannel)
            .copy(description = "0:00 Intro\n2:00 Main part\n5:00 Questions")
    private val searchRepository =
        FakeSearchRepository(SearchSession("q", SearchOrder.RELEVANCE, null), listOf(foundVideo, chapteredVideo))

    private fun createViewModel(
        videoId: String,
        playlistId: PlaylistId? = null,
    ) = PlayerViewModel(
        savedStateHandle =
            SavedStateHandle(
                mapOf(
                    PlayerDestination.VIDEO_ID_ARGUMENT to videoId,
                    PlayerDestination.PLAYLIST_ID_ARGUMENT to playlistId?.value,
                ),
            ),
        observeVideo =
            ObserveVideoWithChaptersUseCase(
                ObserveVideoUseCase(
                    FakeVideoRepository(listOf(video)),
                    searchRepository,
                    playlistRepository,
                    historyRepository,
                    FakeRecommendationRepository(),
                ),
                ParseChaptersUseCase(),
            ),
        observeChannels = ObserveChannelsUseCase(channelRepository),
        observePlaylist = ObservePlaylistUseCase(playlistRepository),
        addChannel = AddChannelUseCase(ChannelInputParser(), channelRepository),
        playbackTracker =
            PlaybackTracker(
                RecordWatchUseCase(historyRepository),
                GetResumePositionUseCase(historyRepository),
                SavePlaybackPositionUseCase(historyRepository),
            ),
    )

    private fun position(
        seconds: Int,
        duration: Duration = 10.minutes,
    ) = PlaybackPosition(seconds.seconds, duration)

    private fun watched(vararg positions: Pair<String, PlaybackPosition>) {
        historyRepository =
            FakeHistoryRepository(
                initialHistory = positions.map { (id, _) -> WatchedVideo(TestData.video(id), watchedAt) },
                initialPositions = positions.associate { (id, position) -> VideoId(id) to position },
            )
    }

    @Test
    fun `shows stored video of a subscribed channel`() =
        runTest {
            val viewModel = createViewModel("abc")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isEqualTo(PlayerUiState.Content(video, isDescriptionExpanded = false, isChannelSubscribed = true))
        }

    @Test
    fun `description can be expanded and collapsed`() =
        runTest {
            val viewModel = createViewModel("abc")
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(PlayerUiEvent.DescriptionToggled)
            advanceUntilIdle()
            assertThat((viewModel.uiState.value as PlayerUiState.Content).isDescriptionExpanded).isEqualTo(true)

            viewModel.onEvent(PlayerUiEvent.DescriptionToggled)
            advanceUntilIdle()
            assertThat((viewModel.uiState.value as PlayerUiState.Content).isDescriptionExpanded).isEqualTo(false)
        }

    @Test
    fun `missing video is not found`() =
        runTest {
            val viewModel = createViewModel("missing")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(PlayerUiState.NotFound)
        }

    @Test
    fun `channel of a found video can be added to the subscriptions`() =
        runTest {
            channelRepository.remoteChannels[ChannelReference.ById(otherChannel.id)] = otherChannel
            val viewModel = createViewModel("found")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            assertThat((viewModel.uiState.value as PlayerUiState.Content).isChannelSubscribed).isEqualTo(false)

            viewModel.onEvent(PlayerUiEvent.SubscribeToChannel)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                PlayerUiState.Content(foundVideo, isDescriptionExpanded = false, isChannelSubscribed = true),
            )
            assertThat(
                channelRepository.getChannels().map { it.id },
            ).isEqualTo(listOf(TestData.firstChannel.id, otherChannel.id))
        }

    @Test
    fun `failed subscription is shown`() =
        runTest {
            channelRepository.addChannelError = ChannelError.Remote(DataError.NoNetwork)
            val viewModel = createViewModel("found")
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(PlayerUiEvent.SubscribeToChannel)
            advanceUntilIdle()

            val state = viewModel.uiState.value as PlayerUiState.Content
            assertThat(state.isChannelSubscribed).isEqualTo(false)
            assertThat(state.isSubscribing).isEqualTo(false)
            assertThat(state.subscribeError).isEqualTo(ChannelError.Remote(DataError.NoNetwork))
        }

    private val PlayerViewModel.content get() = uiState.value as PlayerUiState.Content

    @Test
    fun `playlist plays in order with previous and next`() =
        runTest {
            val videos = listOf(TestData.video("p1"), TestData.video("p2"), TestData.video("p3"))
            val playlistId = playlistRepository.givenPlaylist("Talks", videos = videos)
            val viewModel = createViewModel("p2", playlistId)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            assertThat(viewModel.content.queue).isEqualTo(PlaybackQueue("Talks", position = 2, size = 3))

            viewModel.onEvent(PlayerUiEvent.NextClicked)
            advanceUntilIdle()
            assertThat(viewModel.content.video.id.value).isEqualTo("p3")
            assertThat(viewModel.content.queue?.hasNext).isEqualTo(false)

            viewModel.onEvent(PlayerUiEvent.VideoEnded)
            advanceUntilIdle()
            assertThat(viewModel.content.video.id.value).isEqualTo("p3")

            viewModel.onEvent(PlayerUiEvent.PreviousClicked)
            viewModel.onEvent(PlayerUiEvent.PreviousClicked)
            advanceUntilIdle()
            assertThat(viewModel.content.video.id.value).isEqualTo("p1")
            assertThat(viewModel.content.queue?.hasPrevious).isEqualTo(false)
        }

    @Test
    fun `ended video goes on with the next one and resets the screen state`() =
        runTest {
            val playlistId =
                playlistRepository.givenPlaylist("Talks", videos = listOf(TestData.video("p1"), TestData.video("p2")))
            val viewModel = createViewModel("p1", playlistId)
            collectInBackground(viewModel.uiState)
            viewModel.onEvent(PlayerUiEvent.DescriptionToggled)

            viewModel.onEvent(PlayerUiEvent.VideoEnded)
            advanceUntilIdle()

            assertThat(viewModel.content.video.id.value).isEqualTo("p2")
            assertThat(viewModel.content.isDescriptionExpanded).isEqualTo(false)
        }

    @Test
    fun `single video has no queue and ignores queue events`() =
        runTest {
            val viewModel = createViewModel("abc")
            collectInBackground(viewModel.uiState)

            viewModel.onEvent(PlayerUiEvent.VideoEnded)
            advanceUntilIdle()

            assertThat(viewModel.content.queue).isNull()
            assertThat(viewModel.content.video).isEqualTo(video)
        }

    @Test
    fun `started playback is recorded once per video`() =
        runTest {
            val playlistId =
                playlistRepository.givenPlaylist("Talks", videos = listOf(TestData.video("p1"), TestData.video("p2")))
            val viewModel = createViewModel("p1", playlistId)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(PlayerUiEvent.PlaybackStarted)
            viewModel.onEvent(PlayerUiEvent.PlaybackStarted)
            viewModel.onEvent(PlayerUiEvent.VideoEnded)
            advanceUntilIdle()
            viewModel.onEvent(PlayerUiEvent.PlaybackStarted)
            advanceUntilIdle()

            assertThat(historyRepository.currentHistory.map { it.video.id.value }).containsExactly("p2", "p1")
        }

    @Test
    fun `opened but not played video is not recorded`() =
        runTest {
            val viewModel = createViewModel("abc")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(historyRepository.currentHistory).isEmpty()
        }

    @Test
    fun `video starts where it stopped last time`() =
        runTest {
            watched("abc" to position(seconds = 125))
            val viewModel = createViewModel("abc")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.content.startPosition).isEqualTo(125.seconds)
        }

    @Test
    fun `position is saved while playing and when stopped`() =
        runTest {
            val viewModel = createViewModel("abc")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            assertThat(viewModel.content.startPosition).isEqualTo(Duration.ZERO)
            viewModel.onEvent(PlayerUiEvent.PlaybackStarted)

            viewModel.onEvent(PlayerUiEvent.PlaybackProgressed(video.id, position(seconds = 3)))
            advanceUntilIdle()
            assertThat(historyRepository.getPlaybackPosition(video.id)).isNull()

            viewModel.onEvent(PlayerUiEvent.PlaybackProgressed(video.id, position(seconds = 6)))
            viewModel.onEvent(PlayerUiEvent.PlaybackProgressed(video.id, position(seconds = 8)))
            advanceUntilIdle()
            assertThat(historyRepository.getPlaybackPosition(video.id)).isEqualTo(position(seconds = 6))

            viewModel.onEvent(PlayerUiEvent.PlaybackStopped)
            advanceUntilIdle()
            assertThat(historyRepository.getPlaybackPosition(video.id)).isEqualTo(position(seconds = 8))
        }

    @Test
    fun `ended video is saved as finished and the next one starts where it stopped`() =
        runTest {
            watched("p1" to position(seconds = 30), "p2" to position(seconds = 90))
            val videos = listOf(TestData.video("p1"), TestData.video("p2"))
            val playlistId = playlistRepository.givenPlaylist("Talks", videos = videos)
            val viewModel = createViewModel("p1", playlistId)
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            assertThat(viewModel.content.startPosition).isEqualTo(30.seconds)
            viewModel.onEvent(PlayerUiEvent.PlaybackStarted)
            viewModel.onEvent(PlayerUiEvent.PlaybackProgressed(VideoId("p1"), position(seconds = 598)))

            viewModel.onEvent(PlayerUiEvent.VideoEnded)
            advanceUntilIdle()

            assertThat(historyRepository.getPlaybackPosition(VideoId("p1"))).isEqualTo(position(seconds = 600))
            assertThat(viewModel.content.video.id.value).isEqualTo("p2")
            assertThat(viewModel.content.startPosition).isEqualTo(90.seconds)
        }

    @Test
    fun `progress of another video and of live streams is not saved`() =
        runTest {
            val live = TestData.video("live", contentKind = VideoContentKind.LIVE_OR_PREMIERE)
            playlistRepository.givenPlaylist("Live", videos = listOf(live))
            val viewModel = createViewModel("live")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            viewModel.onEvent(PlayerUiEvent.PlaybackStarted)

            viewModel.onEvent(PlayerUiEvent.PlaybackProgressed(live.id, position(seconds = 60)))
            viewModel.onEvent(PlayerUiEvent.PlaybackProgressed(video.id, position(seconds = 60)))
            viewModel.onEvent(PlayerUiEvent.PlaybackStopped)
            advanceUntilIdle()

            assertThat(historyRepository.currentHistory.map { it.video.id }).containsExactly(live.id)
            assertThat(historyRepository.observePlaybackPositions().first()).isEmpty()
        }

    @Test
    fun `chapters come from the description and the first one is current at the start`() =
        runTest {
            val viewModel = createViewModel("chaptered")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            val content = viewModel.uiState.value as PlayerUiState.Content
            assertThat(content.chapters).containsExactly(
                Chapter("Intro", Duration.ZERO),
                Chapter("Main part", 2.minutes),
                Chapter("Questions", 5.minutes),
            )
            assertThat(content.currentChapterIndex).isEqualTo(0)
        }

    @Test
    fun `a video without a chapter list has no chapters`() =
        runTest {
            val viewModel = createViewModel("abc")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            val content = viewModel.uiState.value as PlayerUiState.Content
            assertThat(content.chapters).isEmpty()
            assertThat(content.currentChapterIndex).isNull()
        }

    @Test
    fun `the current chapter follows the playback and a selected chapter`() =
        runTest {
            val viewModel = createViewModel("chaptered")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(PlayerUiEvent.PlaybackProgressed(chapteredVideo.id, position(seconds = 130)))
            advanceUntilIdle()
            assertThat((viewModel.uiState.value as PlayerUiState.Content).currentChapterIndex).isEqualTo(1)

            viewModel.onEvent(PlayerUiEvent.ChapterSelected(2))
            advanceUntilIdle()
            assertThat((viewModel.uiState.value as PlayerUiState.Content).currentChapterIndex).isEqualTo(2)

            viewModel.onEvent(PlayerUiEvent.ChapterSelected(7))
            advanceUntilIdle()
            assertThat((viewModel.uiState.value as PlayerUiState.Content).currentChapterIndex).isEqualTo(2)
        }

    @Test
    fun `a resumed video starts in the chapter where it stopped`() =
        runTest {
            historyRepository =
                FakeHistoryRepository(
                    initialHistory = listOf(WatchedVideo(chapteredVideo, watchedAt)),
                    initialPositions = mapOf(chapteredVideo.id to position(seconds = 310)),
                )
            val viewModel = createViewModel("chaptered")
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat((viewModel.uiState.value as PlayerUiState.Content).currentChapterIndex).isEqualTo(2)
        }
}
