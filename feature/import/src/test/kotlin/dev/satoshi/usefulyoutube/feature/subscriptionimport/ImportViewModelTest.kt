package dev.satoshi.usefulyoutube.feature.subscriptionimport

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.usecase.ImportSubscriptionsUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ImportError
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ImportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fileContent = CompletableDeferred<AppResult<List<Channel>, ImportError>>()
    private val viewModel =
        ImportViewModel(
            ImportSubscriptionsUseCase(
                fileReader = { fileContent.await() },
                channelRepository = FakeChannelRepository(),
            ),
        )

    @Test
    fun `successful import reports number of new channels`() =
        runTest {
            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(ImportUiState.Idle)

                viewModel.onEvent(ImportUiEvent.FileSelected("content://subscriptions.csv"))
                assertThat(awaitItem()).isEqualTo(ImportUiState.Importing)

                fileContent.complete(AppResult.Success(listOf(TestData.firstChannel, TestData.secondChannel)))
                assertThat(awaitItem()).isEqualTo(ImportUiState.Imported(newChannelCount = 2))
            }
        }

    @Test
    fun `failed import shows error`() =
        runTest {
            fileContent.complete(AppResult.Failure(ImportError.InvalidFormat))

            viewModel.onEvent(ImportUiEvent.FileSelected("content://broken.csv"))

            assertThat(viewModel.uiState.value).isEqualTo(ImportUiState.Failed(ImportError.InvalidFormat))
        }
}
