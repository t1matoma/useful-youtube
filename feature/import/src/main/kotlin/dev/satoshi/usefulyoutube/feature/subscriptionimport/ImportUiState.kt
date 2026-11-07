package dev.satoshi.usefulyoutube.feature.subscriptionimport

import android.content.IntentSender
import dev.satoshi.usefulyoutube.core.model.ImportError

sealed interface ImportUiState {
    data object Idle : ImportUiState

    data object Importing : ImportUiState

    /** The screen has to launch the Google consent screen and report back with [ImportUiEvent.ConsentFinished]. */
    data class ConsentRequired(
        val consentScreen: IntentSender,
    ) : ImportUiState

    data class Imported(
        val newChannelCount: Int,
    ) : ImportUiState

    data class Failed(
        val error: ImportError,
    ) : ImportUiState
}
