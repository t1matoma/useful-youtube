package dev.satoshi.usefulyoutube.feature.subscriptionimport.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.satoshi.usefulyoutube.feature.subscriptionimport.ImportRoute
import kotlinx.serialization.Serializable

@Serializable
data object ImportDestination

fun NavController.navigateToImport() = navigate(ImportDestination)

fun NavGraphBuilder.importScreen(
    onNavigateBack: () -> Unit,
    onOpenFeed: () -> Unit,
) {
    composable<ImportDestination> {
        ImportRoute(onNavigateBack = onNavigateBack, onOpenFeed = onOpenFeed)
    }
}
