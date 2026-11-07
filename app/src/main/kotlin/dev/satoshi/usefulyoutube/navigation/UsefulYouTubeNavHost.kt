package dev.satoshi.usefulyoutube.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import dev.satoshi.usefulyoutube.feature.history.navigation.historyScreen
import dev.satoshi.usefulyoutube.feature.player.navigation.navigateToPlayer
import dev.satoshi.usefulyoutube.feature.player.navigation.playerScreen
import dev.satoshi.usefulyoutube.feature.playlists.navigation.navigateToPlaylist
import dev.satoshi.usefulyoutube.feature.playlists.navigation.playlistScreen
import dev.satoshi.usefulyoutube.feature.playlists.navigation.playlistsScreen
import dev.satoshi.usefulyoutube.feature.search.navigation.searchScreen
import dev.satoshi.usefulyoutube.feature.subscriptionimport.navigation.importScreen
import dev.satoshi.usefulyoutube.feature.subscriptionimport.navigation.navigateToImport

private val TabDotSize = 4.dp
private val TabDotSpacing = 4.dp

@Composable
fun UsefulYouTubeNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isIn(it) }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentTab != null) {
                BottomBar(currentTab = currentTab, onTabClick = navController::navigateToTab)
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
            historyScreen(onVideoClick = navController::navigateToPlayer)
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

@Composable
private fun BottomBar(
    currentTab: TopLevelDestination,
    onTabClick: (TopLevelDestination) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            indicatorColor = Color.Transparent,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        )
    Column {
        HorizontalDivider(color = colors.outlineVariant)
        NavigationBar(containerColor = colors.surfaceContainerLow) {
            TopLevelDestination.entries.forEach { tab ->
                val selected = tab == currentTab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabClick(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { TabLabel(text = stringResource(tab.label), selected = selected) },
                    colors = itemColors,
                )
            }
        }
    }
}

/** The label of a tab; the selected one has a dot under it instead of a filled indicator. */
@Composable
private fun TabLabel(
    text: String,
    selected: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TabDotSpacing),
    ) {
        Text(text)
        Box(
            modifier =
                Modifier
                    .size(TabDotSize)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
        )
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
