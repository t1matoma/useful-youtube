package dev.satoshi.usefulyoutube.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.satoshi.usefulyoutube.feature.channels.navigation.channelsScreen
import dev.satoshi.usefulyoutube.feature.channels.navigation.navigateToChannels
import dev.satoshi.usefulyoutube.feature.feed.navigation.FeedDestination
import dev.satoshi.usefulyoutube.feature.feed.navigation.feedScreen
import dev.satoshi.usefulyoutube.feature.player.navigation.navigateToPlayer
import dev.satoshi.usefulyoutube.feature.player.navigation.playerScreen
import dev.satoshi.usefulyoutube.feature.playlists.navigation.navigateToPlaylist
import dev.satoshi.usefulyoutube.feature.playlists.navigation.playlistScreen
import dev.satoshi.usefulyoutube.feature.playlists.navigation.playlistsScreen
import dev.satoshi.usefulyoutube.feature.search.navigation.searchScreen
import dev.satoshi.usefulyoutube.feature.subscriptionimport.navigation.importScreen
import dev.satoshi.usefulyoutube.feature.subscriptionimport.navigation.navigateToImport

@Composable
fun UsefulYouTubeNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isIn(it) }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Screens have their own scaffolds; consuming the insets keeps them from padding twice.
        NavHost(
            navController = navController,
            startDestination = FeedDestination,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            feedScreen(
                onVideoClick = navController::navigateToPlayer,
                onOpenChannels = navController::navigateToChannels,
                onImportSubscriptions = navController::navigateToImport,
            )
            searchScreen(onVideoClick = navController::navigateToPlayer)
            playlistsScreen(onPlaylistClick = navController::navigateToPlaylist)
            playlistScreen(
                onNavigateBack = navController::popBackStack,
                onPlay = navController::navigateToPlayer,
            )
            playerScreen(onNavigateBack = navController::popBackStack)
            channelsScreen(
                onNavigateBack = navController::popBackStack,
                onImportSubscriptions = navController::navigateToImport,
            )
            importScreen(
                onNavigateBack = navController::popBackStack,
                onOpenFeed = { navController.popBackStack(FeedDestination, inclusive = false) },
            )
        }
    }
}

private fun NavDestination?.isIn(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

/** Tabs keep their own back stack and state, like in the YouTube app. */
private fun NavController.navigateToTab(tab: TopLevelDestination) =
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
