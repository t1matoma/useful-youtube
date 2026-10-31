package dev.satoshi.usefulyoutube.feature.subscriptionimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.domain.usecase.ImportSubscriptionsUseCase
import dev.satoshi.usefulyoutube.core.model.AppResult
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
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
        val uiState: StateFlow<ImportUiState> = mutableUiState.asStateFlow()

        fun onEvent(event: ImportUiEvent) {
            when (event) {
                is ImportUiEvent.FileSelected -> importFrom(event.fileUri)
            }
        }

        private fun importFrom(fileUri: String) {
            if (mutableUiState.value == ImportUiState.Importing) return
            mutableUiState.value = ImportUiState.Importing
            viewModelScope.launch {
                mutableUiState.value =
                    when (val result = importSubscriptions(fileUri)) {
                        is AppResult.Success -> ImportUiState.Imported(result.value)
                        is AppResult.Failure -> ImportUiState.Failed(result.error)
                    }
            }
        }
    }
