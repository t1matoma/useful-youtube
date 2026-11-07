package dev.satoshi.usefulyoutube.feature.player.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dev.satoshi.usefulyoutube.core.model.VideoId

private const val SHOW_CONTROLS = 1
private const val RELATED_VIDEOS_FROM_SAME_CHANNEL_ONLY = 0
private const val HIDE_ANNOTATIONS = 3
private const val START_SECONDS = 0f

/** Loads a video once the player is ready and every time [videoId] changes afterwards. */
private class PlayerController {
    private var player: YouTubePlayer? = null
    private var loadedVideoId: String? = null

    fun onReady(
        readyPlayer: YouTubePlayer,
        videoId: String,
    ) {
        player = readyPlayer
        load(videoId)
    }

    fun load(videoId: String) {
        val readyPlayer = player ?: return
        if (videoId == loadedVideoId) return
        loadedVideoId = videoId
        readyPlayer.loadVideo(videoId, START_SECONDS)
    }
}

/** Official YouTube IFrame player that plays [videoId] and reports when playback starts and ends. */
@Composable
internal fun YouTubePlayer(
    videoId: VideoId,
    onPlaybackStarted: () -> Unit,
    onVideoEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val controller = remember { PlayerController() }
    val currentVideoId by rememberUpdatedState(videoId)
    val currentOnPlaybackStarted by rememberUpdatedState(onPlaybackStarted)
    val currentOnVideoEnded by rememberUpdatedState(onVideoEnded)
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
                            controller.onReady(youTubePlayer, currentVideoId.value)
                        }

                        override fun onStateChange(
                            youTubePlayer: YouTubePlayer,
                            state: PlayerConstants.PlayerState,
                        ) {
                            when (state) {
                                PlayerConstants.PlayerState.PLAYING -> {
                                    currentOnPlaybackStarted()
                                }

                                PlayerConstants.PlayerState.ENDED -> {
                                    currentOnVideoEnded()
                                }

                                else -> {}
                            }
                        }
                    },
                    options,
                )
            }
        },
        update = { controller.load(videoId.value) },
        onRelease = { playerView ->
            lifecycle.removeObserver(playerView)
            playerView.release()
        },
    )
}
