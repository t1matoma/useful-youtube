package dev.satoshi.usefulyoutube.feature.channels

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelInputParser
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.domain.usecase.AddChannelUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveChannelsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RemoveChannelUseCase
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ChannelsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeChannelRepository(listOf(TestData.firstChannel))

    private fun TestScope.createViewModel(): ChannelsViewModel {
        val viewModel =
            ChannelsViewModel(
                observeChannels = ObserveChannelsUseCase(repository),
                addChannel = AddChannelUseCase(ChannelInputParser(), repository),
                removeChannel = RemoveChannelUseCase(repository),
            )
        collectInBackground(viewModel.uiState)
        return viewModel
    }

    @Test
    fun `shows subscribed channels`() =
        runTest {
            val state = createViewModel().uiState.value

            assertThat(state.isLoading).isEqualTo(false)
            assertThat(state.channels).containsExactly(TestData.firstChannel)
        }

    @Test
    fun `adds channel and clears input`() =
        runTest {
            repository.remoteChannels[ChannelReference.ByHandle("second")] = TestData.secondChannel
            val viewModel = createViewModel()

            viewModel.onEvent(ChannelsUiEvent.InputChanged("@second"))
            viewModel.onEvent(ChannelsUiEvent.AddClicked)

            val state = viewModel.uiState.value
            assertThat(state.channels).containsExactly(TestData.firstChannel, TestData.secondChannel)
            assertThat(state.input).isEqualTo("")
            assertThat(state.isAdding).isEqualTo(false)
        }

    @Test
    fun `invalid input shows error until input changes`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEvent(ChannelsUiEvent.InputChanged("not a channel"))
            viewModel.onEvent(ChannelsUiEvent.AddClicked)
            assertThat(viewModel.uiState.value.addError).isEqualTo(ChannelError.InvalidInput)
            assertThat(viewModel.uiState.value.input).isEqualTo("not a channel")

            viewModel.onEvent(ChannelsUiEvent.InputChanged("@fixed"))
            assertThat(viewModel.uiState.value.addError).isNull()
        }

    @Test
    fun `blank input is ignored`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEvent(ChannelsUiEvent.AddClicked)

            assertThat(viewModel.uiState.value.addError).isNull()
        }

    @Test
    fun `channel is removed only after confirmation`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEvent(ChannelsUiEvent.RemoveRequested(TestData.firstChannel))
            assertThat(viewModel.uiState.value.channelPendingRemoval).isEqualTo(TestData.firstChannel)

            viewModel.onEvent(ChannelsUiEvent.RemoveDismissed)
            assertThat(viewModel.uiState.value.channels).containsExactly(TestData.firstChannel)

            viewModel.onEvent(ChannelsUiEvent.RemoveRequested(TestData.firstChannel))
            viewModel.onEvent(ChannelsUiEvent.RemoveConfirmed)
            assertThat(viewModel.uiState.value.channels).containsExactly()
            assertThat(viewModel.uiState.value.channelPendingRemoval).isNull()
        }
}
