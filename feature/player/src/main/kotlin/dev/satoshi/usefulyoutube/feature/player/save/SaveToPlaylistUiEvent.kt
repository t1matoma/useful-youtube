package dev.satoshi.usefulyoutube.feature.player.save

import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.VideoId

sealed interface SaveToPlaylistUiEvent {
    /** The player switched to another video of a playlist. */
    data class VideoChanged(
        val videoId: VideoId,
    ) : SaveToPlaylistUiEvent

    data object SaveClicked : SaveToPlaylistUiEvent

    data object SheetDismissed : SaveToPlaylistUiEvent

    data class PlaylistToggled(
        val id: PlaylistId,
        val containsVideo: Boolean,
    ) : SaveToPlaylistUiEvent

    data object NewPlaylistClicked : SaveToPlaylistUiEvent

    data class NewPlaylistTitleChanged(
        val title: String,
    ) : SaveToPlaylistUiEvent

    data object NewPlaylistConfirmed : SaveToPlaylistUiEvent

    data object NewPlaylistDismissed : SaveToPlaylistUiEvent
}
