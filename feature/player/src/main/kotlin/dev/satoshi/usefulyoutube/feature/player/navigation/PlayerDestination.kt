package dev.satoshi.usefulyoutube.feature.player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.player.PlayerRoute
import kotlinx.serialization.Serializable

/** With a [playlistId] the player plays the playlist from [videoId] on. */
@Serializable
data class PlayerDestination(
    val videoId: String,
    val playlistId: Long? = null,
) {
    companion object {
        /** Names of the arguments in the saved state; they match the property names. */
        const val VIDEO_ID_ARGUMENT = "videoId"
        const val PLAYLIST_ID_ARGUMENT = "playlistId"
    }
}

fun NavController.navigateToPlayer(videoId: VideoId) = navigate(PlayerDestination(videoId.value))

fun NavController.navigateToPlayer(
    videoId: VideoId,
    playlistId: PlaylistId,
) = navigate(PlayerDestination(videoId.value, playlistId.value))

fun NavGraphBuilder.playerScreen(onNavigateBack: () -> Unit) {
    composable<PlayerDestination> {
        PlayerRoute(onNavigateBack = onNavigateBack)
    }
}
