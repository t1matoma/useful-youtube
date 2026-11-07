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
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveChannelsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObservePlaylistUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RecordWatchUseCase
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val video = TestData.video(id = "abc")
    private val otherChannel = Channel(ChannelId("UCcccccccccccccccccccccc"), "Other channel")
    private val foundVideo = TestData.video(id = "found", channel = otherChannel)
    private val channelRepository = FakeChannelRepository(listOf(TestData.firstChannel))
    private val playlistRepository = FakePlaylistRepository()
    private val historyRepository = FakeHistoryRepository()
    private val searchRepository =
        FakeSearchRepository(SearchSession("q", SearchOrder.RELEVANCE, null), listOf(foundVideo))

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
            ObserveVideoUseCase(
                FakeVideoRepository(listOf(video)),
                searchRepository,
                playlistRepository,
                historyRepository,
            ),
        observeChannels = ObserveChannelsUseCase(channelRepository),
        observePlaylist = ObservePlaylistUseCase(playlistRepository),
        addChannel = AddChannelUseCase(ChannelInputParser(), channelRepository),
        recordWatch = RecordWatchUseCase(historyRepository),
    )

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
}
