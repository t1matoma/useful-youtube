package dev.satoshi.usefulyoutube.feature.playlists.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.playlists.PlaylistRoute
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDestination(
    val playlistId: Long,
) {
    companion object {
        /** Name of the argument in the saved state; matches the [playlistId] property. */
        const val PLAYLIST_ID_ARGUMENT = "playlistId"
    }
}

fun NavController.navigateToPlaylist(id: PlaylistId) = navigate(PlaylistDestination(id.value))

fun NavGraphBuilder.playlistScreen(
    onNavigateBack: () -> Unit,
    onVideoClick: (VideoId) -> Unit,
) {
    composable<PlaylistDestination> {
        PlaylistRoute(onNavigateBack = onNavigateBack, onVideoClick = onVideoClick)
    }
}
