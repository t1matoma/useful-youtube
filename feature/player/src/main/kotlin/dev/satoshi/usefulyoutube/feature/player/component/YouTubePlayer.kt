package dev.satoshi.usefulyoutube.feature.player.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dev.satoshi.usefulyoutube.core.model.VideoId

private const val SHOW_CONTROLS = 1
private const val RELATED_VIDEOS_FROM_SAME_CHANNEL_ONLY = 0
private const val HIDE_ANNOTATIONS = 3
private const val START_SECONDS = 0f

/** Official YouTube IFrame player that starts playing [videoId]. */
@Composable
internal fun YouTubePlayer(
    videoId: VideoId,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        modifier = modifier,
        factory = { context ->
            YouTubePlayerView(context).apply {
                enableAutomaticInitialization = false
                lifecycle.addObserver(this)
                val options =
                    IFramePlayerOptions
                        .Builder(context)
                        .controls(SHOW_CONTROLS)
                        .rel(RELATED_VIDEOS_FROM_SAME_CHANNEL_ONLY)
                        .ivLoadPolicy(HIDE_ANNOTATIONS)
                        .build()
                initialize(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId.value, START_SECONDS)
                        }
                    },
                    options,
                )
            }
        },
        onRelease = { playerView ->
            lifecycle.removeObserver(playerView)
            playerView.release()
        },
    )
}
