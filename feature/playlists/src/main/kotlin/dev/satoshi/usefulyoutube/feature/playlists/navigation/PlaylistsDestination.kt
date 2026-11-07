package dev.satoshi.usefulyoutube.feature.playlists.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.feature.playlists.PlaylistsRoute
import kotlinx.serialization.Serializable

@Serializable
data object PlaylistsDestination

fun NavGraphBuilder.playlistsScreen(onPlaylistClick: (PlaylistId) -> Unit) {
    composable<PlaylistsDestination> {
        PlaylistsRoute(onPlaylistClick = onPlaylistClick)
    }
}
