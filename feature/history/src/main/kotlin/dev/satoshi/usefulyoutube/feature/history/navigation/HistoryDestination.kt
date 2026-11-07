package dev.satoshi.usefulyoutube.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.history.HistoryRoute
import kotlinx.serialization.Serializable

@Serializable
data object HistoryDestination

fun NavGraphBuilder.historyScreen(onVideoClick: (VideoId) -> Unit) {
    composable<HistoryDestination> {
        HistoryRoute(onVideoClick = onVideoClick)
    }
}
