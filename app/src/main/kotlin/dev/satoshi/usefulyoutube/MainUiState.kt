package dev.satoshi.usefulyoutube

data class MainUiState(
    /** Whether the bottom bar offers the "For you" tab; on until the settings say otherwise. */
    val recommendationsEnabled: Boolean = true,
)
