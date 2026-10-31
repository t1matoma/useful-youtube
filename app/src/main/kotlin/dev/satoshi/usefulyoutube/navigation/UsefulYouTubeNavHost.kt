package dev.satoshi.usefulyoutube.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.satoshi.usefulyoutube.feature.feed.navigation.FeedDestination
import dev.satoshi.usefulyoutube.feature.feed.navigation.feedScreen
import dev.satoshi.usefulyoutube.feature.player.navigation.navigateToPlayer
import dev.satoshi.usefulyoutube.feature.player.navigation.playerScreen
import dev.satoshi.usefulyoutube.feature.subscriptionimport.navigation.importScreen
import dev.satoshi.usefulyoutube.feature.subscriptionimport.navigation.navigateToImport

@Composable
fun UsefulYouTubeNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = FeedDestination, modifier = modifier) {
        feedScreen(
            onVideoClick = navController::navigateToPlayer,
            onOpenChannels = {},
            onImportSubscriptions = navController::navigateToImport,
        )
        playerScreen(onNavigateBack = navController::popBackStack)
        importScreen(
            onNavigateBack = navController::popBackStack,
            onOpenFeed = { navController.popBackStack(FeedDestination, inclusive = false) },
        )
    }
}
