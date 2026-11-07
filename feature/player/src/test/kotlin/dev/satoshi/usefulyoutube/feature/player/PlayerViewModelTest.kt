package dev.satoshi.usefulyoutube.feature.player

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelInputParser
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.domain.usecase.AddChannelUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveChannelsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
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
    private val searchRepository =
        FakeSearchRepository(SearchSession("q", SearchOrder.RELEVANCE, null), listOf(foundVideo))

    private fun createViewModel(videoId: String) =
        PlayerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PlayerDestination.VIDEO_ID_ARGUMENT to videoId)),
            observeVideo =
                ObserveVideoUseCase(
                    FakeVideoRepository(listOf(video)),
                    searchRepository,
                    playlistRepository,
                ),
            observeChannels = ObserveChannelsUseCase(channelRepository),
            addChannel = AddChannelUseCase(ChannelInputParser(), channelRepository),
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
}
