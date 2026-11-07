package dev.satoshi.usefulyoutube.feature.player.component

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val SideControlsPadding = 32.dp

/**
 * Shows the player's fullscreen [video] over the whole screen in landscape with the system bars hidden.
 * Back leaves fullscreen. [sideControls] are spread across the middle of the screen, for example previous on
 * the left and next on the right.
 */
@Composable
internal fun FullscreenPlayer(
    video: FullscreenVideo,
    modifier: Modifier = Modifier,
    sideControls: (@Composable RowScope.() -> Unit)? = null,
) {
    ImmersiveLandscapeEffect()
    BackHandler(onBack = video.exit)
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        key(video.view) {
            AndroidView(factory = { video.view }, modifier = Modifier.fillMaxSize())
        }
        if (sideControls != null) {
            Row(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = SideControlsPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                content = sideControls,
            )
        }
    }
}

/** Turns the screen to landscape and hides the system bars until it leaves the composition. */
@Composable
private fun ImmersiveLandscapeEffect() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val previousOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = previousOrientation
        }
    }
}
