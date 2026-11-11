package dev.satoshi.usefulyoutube.feature.recommendations.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.recommendations.RecommendationsRoute
import kotlinx.serialization.Serializable

@Serializable
data object RecommendationsDestination

fun NavGraphBuilder.recommendationsScreen(onVideoClick: (VideoId) -> Unit) {
    composable<RecommendationsDestination> {
        RecommendationsRoute(onVideoClick = onVideoClick)
    }
}
