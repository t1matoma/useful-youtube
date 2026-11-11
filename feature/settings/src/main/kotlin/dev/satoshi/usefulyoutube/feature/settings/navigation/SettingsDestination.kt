package dev.satoshi.usefulyoutube.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

fun NavController.navigateToSettings() = navigate(SettingsDestination)

fun NavGraphBuilder.settingsScreen(onNavigateBack: () -> Unit) {
    composable<SettingsDestination> {
        SettingsRoute(onNavigateBack = onNavigateBack)
    }
}
