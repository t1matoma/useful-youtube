package dev.satoshi.usefulyoutube.feature.subscriptionimport

sealed interface ImportUiEvent {
    data class FileSelected(
        val fileUri: String,
    ) : ImportUiEvent

    data object AccountImportRequested : ImportUiEvent

    /** The consent screen from [ImportUiState.ConsentRequired] was launched. */
    data object ConsentLaunched : ImportUiEvent

    data class ConsentFinished(
        val granted: Boolean,
    ) : ImportUiEvent
}
