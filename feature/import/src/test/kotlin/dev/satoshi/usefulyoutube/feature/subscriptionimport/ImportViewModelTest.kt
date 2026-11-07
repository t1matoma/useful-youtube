package dev.satoshi.usefulyoutube.feature.subscriptionimport

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.repository.AccountSubscriptionsReader
import dev.satoshi.usefulyoutube.core.domain.usecase.ImportAccountSubscriptionsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ImportSubscriptionsUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.ImportError
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeAccountAuthorizer
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ImportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fileContent = CompletableDeferred<AppResult<List<Channel>, ImportError>>()
    private val authorizer = FakeAccountAuthorizer()
    private var accountSubscriptions: AppResult<List<Channel>, DataError> =
        AppResult.Success(listOf(TestData.firstChannel, TestData.secondChannel))
    private var consentScreen: AppResult<IntentSender?, AuthError> = AppResult.Success(null)
    private val channelRepository = FakeChannelRepository()

    private val viewModel =
        ImportViewModel(
            importSubscriptions =
                ImportSubscriptionsUseCase(
                    fileReader = { fileContent.await() },
                    channelRepository = channelRepository,
                ),
            importAccountSubscriptions =
                ImportAccountSubscriptionsUseCase(
                    authorizer = authorizer,
                    subscriptionsReader = AccountSubscriptionsReader { accountSubscriptions },
                    channelRepository = channelRepository,
                ),
            accountConsent = { consentScreen },
        )

    private fun intentSender(): IntentSender =
        PendingIntent
            .getActivity(RuntimeEnvironment.getApplication(), 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            .intentSender

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

    @Test
    fun `authorized account import reports number of new channels`() =
        runTest {
            viewModel.onEvent(ImportUiEvent.AccountImportRequested)

            assertThat(viewModel.uiState.value).isEqualTo(ImportUiState.Imported(newChannelCount = 2))
        }

    @Test
    fun `account import asks for consent and continues after it is granted`() =
        runTest {
            val sender = intentSender()
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Success(sender)

            viewModel.onEvent(ImportUiEvent.AccountImportRequested)
            assertThat(viewModel.uiState.value).isEqualTo(ImportUiState.ConsentRequired(sender))

            viewModel.onEvent(ImportUiEvent.ConsentLaunched)
            assertThat(viewModel.uiState.value).isEqualTo(ImportUiState.Importing)

            authorizer.error = null
            viewModel.onEvent(ImportUiEvent.ConsentFinished(granted = true))
            assertThat(viewModel.uiState.value).isEqualTo(ImportUiState.Imported(newChannelCount = 2))
        }

    @Test
    fun `declined consent is reported as cancelled`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Success(intentSender())
            viewModel.onEvent(ImportUiEvent.AccountImportRequested)
            viewModel.onEvent(ImportUiEvent.ConsentLaunched)

            viewModel.onEvent(ImportUiEvent.ConsentFinished(granted = false))

            assertThat(viewModel.uiState.value)
                .isEqualTo(ImportUiState.Failed(ImportError.Auth(AuthError.Cancelled)))
        }

    @Test
    fun `consent result without a launched consent screen is ignored`() =
        runTest {
            viewModel.onEvent(ImportUiEvent.ConsentFinished(granted = true))

            assertThat(viewModel.uiState.value).isEqualTo(ImportUiState.Idle)
        }

    @Test
    fun `consent that is still required after granting is reported instead of shown again`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Success(null)

            viewModel.onEvent(ImportUiEvent.AccountImportRequested)

            assertThat(viewModel.uiState.value)
                .isEqualTo(ImportUiState.Failed(ImportError.Auth(AuthError.ConsentRequired)))
        }

    @Test
    fun `unavailable play services are reported`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Failure(AuthError.Unavailable)

            viewModel.onEvent(ImportUiEvent.AccountImportRequested)

            assertThat(viewModel.uiState.value)
                .isEqualTo(ImportUiState.Failed(ImportError.Auth(AuthError.Unavailable)))
        }

    @Test
    fun `network error of account import is reported`() =
        runTest {
            accountSubscriptions = AppResult.Failure(DataError.NoNetwork)

            viewModel.onEvent(ImportUiEvent.AccountImportRequested)

            assertThat(viewModel.uiState.value)
                .isEqualTo(ImportUiState.Failed(ImportError.Remote(DataError.NoNetwork)))
        }
}
