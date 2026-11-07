package dev.satoshi.usefulyoutube.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import dev.satoshi.usefulyoutube.R
import dev.satoshi.usefulyoutube.feature.feed.navigation.FeedDestination
import dev.satoshi.usefulyoutube.feature.playlists.navigation.PlaylistsDestination
import dev.satoshi.usefulyoutube.feature.search.navigation.SearchDestination
import kotlin.reflect.KClass

/** Screens of the bottom navigation bar; other screens open on top of them without the bar. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @param:StringRes val label: Int,
) {
    FEED(FeedDestination, FeedDestination::class, Icons.Default.Home, R.string.app_tab_feed),
    SEARCH(SearchDestination, SearchDestination::class, Icons.Default.Search, R.string.app_tab_search),
    PLAYLISTS(
        PlaylistsDestination,
        PlaylistsDestination::class,
        Icons.AutoMirrored.Filled.List,
        R.string.app_tab_playlists,
    ),
}
