package dev.satoshi.usefulyoutube.feature.channels.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.feature.channels.ChannelsRoute
import kotlinx.serialization.Serializable

@Serializable
data object ChannelsDestination

fun NavController.navigateToChannels() = navigate(ChannelsDestination)

fun NavGraphBuilder.channelsScreen(
    onNavigateBack: () -> Unit,
    onImportSubscriptions: () -> Unit,
) {
    composable<ChannelsDestination> {
        ChannelsRoute(onNavigateBack = onNavigateBack, onImportSubscriptions = onImportSubscriptions)
    }
}
