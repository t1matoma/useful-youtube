package dev.satoshi.usefulyoutube.feature.player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.player.PlayerRoute
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDestination(
    val videoId: String,
) {
    companion object {
        /** Name of the argument in the saved state; matches the [videoId] property. */
        const val VIDEO_ID_ARGUMENT = "videoId"
    }
}

fun NavController.navigateToPlayer(videoId: VideoId) = navigate(PlayerDestination(videoId.value))

fun NavGraphBuilder.playerScreen(onNavigateBack: () -> Unit) {
    composable<PlayerDestination> {
        PlayerRoute(onNavigateBack = onNavigateBack)
    }
}
