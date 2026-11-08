package dev.satoshi.usefulyoutube.feature.player.component

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import dev.satoshi.usefulyoutube.feature.player.R

private const val ACTION_PLAY_PAUSE = "dev.satoshi.usefulyoutube.feature.player.PIP_PLAY_PAUSE"
private const val PLAY_PAUSE_REQUEST_CODE = 1
private val VideoAspectRatio = Rational(16, 9)

/** Whether the activity is shown as a picture-in-picture window now. */
@Composable
internal fun rememberIsInPictureInPictureMode(): Boolean {
    val activity = LocalActivity.current as? ComponentActivity ?: return false
    var isInPictureInPicture by remember(activity) { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val listener =
            Consumer<PictureInPictureModeChangedInfo> { isInPictureInPicture = it.isInPictureInPictureMode }
        activity.addOnPictureInPictureModeChangedListener(listener)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return isInPictureInPicture
}

/**
 * While the video [isPlaying], leaving the app (Home, the recent apps) turns the activity into a picture-in-picture
 * window with the video. The window has a play/pause button that calls [onPlay] or [onPause]. [videoBounds] is
 * where the video is in the window, for a smooth animation. Nothing happens on devices without picture-in-picture.
 */
@Composable
internal fun PictureInPictureEffect(
    isPlaying: Boolean,
    videoBounds: Rect?,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentOnPlay by rememberUpdatedState(onPlay)
    val currentOnPause by rememberUpdatedState(onPause)
    val params = remember(activity, isPlaying, videoBounds) { pictureInPictureParams(activity, isPlaying, videoBounds) }
    LaunchedEffect(activity, params) { activity.setPictureInPictureParams(params) }
    DisposableEffect(activity) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (currentIsPlaying) currentOnPause() else currentOnPlay()
                }
            }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(ACTION_PLAY_PAUSE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // Android 12 and newer enter by themselves thanks to setAutoEnterEnabled.
        val onUserLeave =
            Runnable {
                if (currentIsPlaying && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    activity.enterPictureInPictureMode(pictureInPictureParams(activity, isPlaying = true, null))
                }
            }
        activity.addOnUserLeaveHintListener(onUserLeave)
        onDispose {
            activity.removeOnUserLeaveHintListener(onUserLeave)
            activity.unregisterReceiver(receiver)
            // Other screens must not turn into a picture-in-picture window.
            activity.setPictureInPictureParams(pictureInPictureParams(activity, isPlaying = false, null))
        }
    }
}

/**
 * The picture-in-picture window of the video: 16:9, entered automatically when the app is left only while the
 * video [isPlaying], with a pause button while it plays and a play button while it is paused.
 */
internal fun pictureInPictureParams(
    context: Context,
    isPlaying: Boolean,
    videoBounds: Rect?,
): PictureInPictureParams {
    val builder =
        PictureInPictureParams
            .Builder()
            .setAspectRatio(VideoAspectRatio)
            .setSourceRectHint(videoBounds)
            .setActions(listOf(playPauseAction(context, isPlaying)))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(isPlaying).setSeamlessResizeEnabled(false)
    }
    return builder.build()
}

private fun playPauseAction(
    context: Context,
    isPlaying: Boolean,
): RemoteAction {
    val title = context.getString(if (isPlaying) R.string.feature_player_pause else R.string.feature_player_play)
    val icon = if (isPlaying) R.drawable.feature_player_ic_pause else R.drawable.feature_player_ic_play
    val intent = Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName)
    val pendingIntent =
        PendingIntent.getBroadcast(context, PLAY_PAUSE_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE)
    return RemoteAction(Icon.createWithResource(context, icon), title, title, pendingIntent)
}
