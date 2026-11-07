package dev.satoshi.usefulyoutube.feature.subscriptionimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.auth.AccountConsent
import dev.satoshi.usefulyoutube.core.domain.usecase.ImportAccountSubscriptionsUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ImportSubscriptionsUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.ImportError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel
    @Inject
    constructor(
        private val importSubscriptions: ImportSubscriptionsUseCase,
        private val importAccountSubscriptions: ImportAccountSubscriptionsUseCase,
        private val accountConsent: AccountConsent,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
        val uiState: StateFlow<ImportUiState> = mutableUiState.asStateFlow()

        /** `true` while the consent screen is open; its result is ignored otherwise. */
        private var awaitingConsent = false

        fun onEvent(event: ImportUiEvent) {
            when (event) {
                is ImportUiEvent.FileSelected -> startImport { importSubscriptions(event.fileUri).toUiState() }
                ImportUiEvent.AccountImportRequested -> startImport { importFromAccount() }
                ImportUiEvent.ConsentLaunched -> onConsentLaunched()
                is ImportUiEvent.ConsentFinished -> onConsentFinished(event.granted)
            }
        }

        private fun startImport(import: suspend () -> ImportUiState) {
            if (isBusy()) return
            mutableUiState.value = ImportUiState.Importing
            viewModelScope.launch { mutableUiState.value = import() }
        }

        private fun isBusy() =
            awaitingConsent ||
                mutableUiState.value == ImportUiState.Importing ||
                mutableUiState.value is ImportUiState.ConsentRequired

        private suspend fun importFromAccount(): ImportUiState {
            val result = importAccountSubscriptions()
            if (result != AppResult.Failure(ImportError.Auth(AuthError.ConsentRequired))) return result.toUiState()
            return when (val consent = accountConsent.consentScreen()) {
                is AppResult.Failure -> ImportUiState.Failed(ImportError.Auth(consent.error))
                is AppResult.Success -> consent.value?.let(ImportUiState::ConsentRequired) ?: retryAfterConsent()
            }
        }

        private fun onConsentLaunched() {
            if (mutableUiState.value !is ImportUiState.ConsentRequired) return
            awaitingConsent = true
            mutableUiState.value = ImportUiState.Importing
        }

        private fun onConsentFinished(granted: Boolean) {
            if (!awaitingConsent) return
            awaitingConsent = false
            if (granted) {
                viewModelScope.launch { mutableUiState.value = retryAfterConsent() }
            } else {
                mutableUiState.value = ImportUiState.Failed(ImportError.Auth(AuthError.Cancelled))
            }
        }

        /** Access is granted now, so a repeated consent request is reported instead of shown again. */
        private suspend fun retryAfterConsent(): ImportUiState = importAccountSubscriptions().toUiState()

        private fun AppResult<Int, ImportError>.toUiState(): ImportUiState =
            when (this) {
                is AppResult.Success -> ImportUiState.Imported(value)
                is AppResult.Failure -> ImportUiState.Failed(error)
            }
    }
