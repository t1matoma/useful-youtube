package dev.satoshi.usefulyoutube.feature.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

fun NavController.navigateToSearch() = navigate(SearchDestination)

fun NavGraphBuilder.searchScreen(
    onNavigateBack: () -> Unit,
    onVideoClick: (VideoId) -> Unit,
) {
    composable<SearchDestination> {
        SearchRoute(onNavigateBack = onNavigateBack, onVideoClick = onVideoClick)
    }
}
