package dev.satoshi.usefulyoutube.feature.player

import dev.satoshi.usefulyoutube.core.model.Video

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    data object NotFound : PlayerUiState

    data class Content(
        val video: Video,
        val isDescriptionExpanded: Boolean,
    ) : PlayerUiState
}
