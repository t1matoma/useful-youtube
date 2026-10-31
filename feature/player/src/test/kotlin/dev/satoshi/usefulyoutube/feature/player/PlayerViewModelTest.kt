package dev.satoshi.usefulyoutube.feature.player

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val video = TestData.video(id = "abc")
    private val repository = FakeVideoRepository(listOf(video))

    private fun createViewModel(videoId: String) =
        PlayerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PlayerDestination.VIDEO_ID_ARGUMENT to videoId)),
            observeVideo = ObserveVideoUseCase(repository),
        )

    @Test
    fun `shows stored video`() =
        runTest {
            createViewModel("abc").uiState.test {
                assertThat(awaitItem()).isEqualTo(PlayerUiState.Content(video, isDescriptionExpanded = false))
            }
        }

    @Test
    fun `description can be expanded and collapsed`() =
        runTest {
            val viewModel = createViewModel("abc")

            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(PlayerUiState.Content(video, isDescriptionExpanded = false))

                viewModel.onEvent(PlayerUiEvent.DescriptionToggled)
                assertThat(awaitItem()).isEqualTo(PlayerUiState.Content(video, isDescriptionExpanded = true))

                viewModel.onEvent(PlayerUiEvent.DescriptionToggled)
                assertThat(awaitItem()).isEqualTo(PlayerUiState.Content(video, isDescriptionExpanded = false))
            }
        }

    @Test
    fun `missing video is not found`() =
        runTest {
            createViewModel("missing").uiState.test {
                assertThat(awaitItem()).isEqualTo(PlayerUiState.NotFound)
            }
        }
}
