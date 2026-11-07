package dev.satoshi.usefulyoutube.feature.feed.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.feature.feed.FeedRoute
import kotlinx.serialization.Serializable

@Serializable
data object FeedDestination

fun NavGraphBuilder.feedScreen(
    onVideoClick: (VideoId) -> Unit,
    onOpenChannels: () -> Unit,
    onImportSubscriptions: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    composable<FeedDestination> {
        FeedRoute(
            onVideoClick = onVideoClick,
            onOpenChannels = onOpenChannels,
            onImportSubscriptions = onImportSubscriptions,
            onOpenSearch = onOpenSearch,
        )
    }
}
