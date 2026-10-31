package dev.satoshi.usefulyoutube.feature.subscriptionimport

sealed interface ImportUiEvent {
    data class FileSelected(
        val fileUri: String,
    ) : ImportUiEvent
}
