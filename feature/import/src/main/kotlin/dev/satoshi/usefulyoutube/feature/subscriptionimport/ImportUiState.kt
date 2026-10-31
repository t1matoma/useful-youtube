package dev.satoshi.usefulyoutube.feature.subscriptionimport

import dev.satoshi.usefulyoutube.core.model.ImportError

sealed interface ImportUiState {
    data object Idle : ImportUiState

    data object Importing : ImportUiState

    data class Imported(
        val newChannelCount: Int,
    ) : ImportUiState

    data class Failed(
        val error: ImportError,
    ) : ImportUiState
}
