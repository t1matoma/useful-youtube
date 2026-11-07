package dev.satoshi.usefulyoutube.feature.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

fun NavGraphBuilder.searchScreen(onVideoClick: (VideoId) -> Unit) {
    composable<SearchDestination> {
        SearchRoute(onVideoClick = onVideoClick)
    }
}
