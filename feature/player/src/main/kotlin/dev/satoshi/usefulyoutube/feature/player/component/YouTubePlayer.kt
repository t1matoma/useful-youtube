package dev.satoshi.usefulyoutube.feature.player.component

import android.view.View
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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val SHOW_CONTROLS = 1
private const val SHOW_FULLSCREEN_BUTTON = 1
private const val RELATED_VIDEOS_FROM_SAME_CHANNEL_ONLY = 0
private const val HIDE_ANNOTATIONS = 3

/**
 * Loads a video once the player is ready and every time the video id changes afterwards; plays and pauses it
 * from outside the player, for example from the picture-in-picture window. Tells where the loaded video is.
 */
internal class PlayerController {
    private var player: YouTubePlayer? = null
    private var loadedVideoId: String? = null

    /** Until the loaded video plays, the player may still report the time of the previous one. */
    private var isLoadedVideoStarted = false
    private var loadedVideoDurationSeconds = 0f

    fun onReady(
        readyPlayer: YouTubePlayer,
        videoId: String,
        start: Duration,
    ) {
        player = readyPlayer
        load(videoId, start)
    }

    /** [start] is used only when another video is loaded. */
    fun load(
        videoId: String,
        start: Duration,
    ) {
        val readyPlayer = player ?: return
        if (videoId == loadedVideoId) return
        loadedVideoId = videoId
        isLoadedVideoStarted = false
        loadedVideoDurationSeconds = 0f
        readyPlayer.loadVideo(videoId, start.toDouble(DurationUnit.SECONDS).toFloat())
    }

    fun onStateChange(state: PlayerConstants.PlayerState) {
        if (state == PlayerConstants.PlayerState.PLAYING) isLoadedVideoStarted = true
    }

    fun onVideoDuration(seconds: Float) {
        loadedVideoDurationSeconds = seconds
    }

    /** `null` until the loaded video plays and while its duration is unknown, for example for a live stream. */
    fun positionAt(second: Float): Pair<VideoId, PlaybackPosition>? {
        val videoId = loadedVideoId ?: return null
        if (!isLoadedVideoStarted || loadedVideoDurationSeconds <= 0f) return null
        return VideoId(videoId) to
            PlaybackPosition(
                position = second.toDouble().toDuration(DurationUnit.SECONDS),
                duration = loadedVideoDurationSeconds.toDouble().toDuration(DurationUnit.SECONDS),
            )
    }

    /** Jumps inside the loaded video, for example to a chapter. */
    fun seekTo(position: Duration) {
        player?.seekTo(position.toDouble(DurationUnit.SECONDS).toFloat())
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }
}

/**
 * The player is in fullscreen: [view] shows the video and must cover the screen; [exit] leaves fullscreen,
 * after which the player calls `onExitFullscreen`.
 */
internal class FullscreenVideo(
    val view: View,
    val exit: () -> Unit,
)

/**
 * Official YouTube IFrame player that plays [videoId] from [startPosition] and reports when playback starts,
 * progresses, pauses and ends, whether it is playing, and when its own fullscreen button is used.
 */
@Composable
internal fun YouTubePlayer(
    videoId: VideoId,
    startPosition: Duration,
    onPlaybackStarted: () -> Unit,
    onProgress: (VideoId, PlaybackPosition) -> Unit,
    onPaused: () -> Unit,
    onVideoEnded: () -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    onEnterFullscreen: (FullscreenVideo) -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    controller: PlayerController = remember { PlayerController() },
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentVideoId by rememberUpdatedState(videoId)
    val currentStartPosition by rememberUpdatedState(startPosition)
    val currentOnPlaybackStarted by rememberUpdatedState(onPlaybackStarted)
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnPaused by rememberUpdatedState(onPaused)
    val currentOnVideoEnded by rememberUpdatedState(onVideoEnded)
    val currentOnPlayingChange by rememberUpdatedState(onPlayingChange)
    val currentOnEnterFullscreen by rememberUpdatedState(onEnterFullscreen)
    val currentOnExitFullscreen by rememberUpdatedState(onExitFullscreen)
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
                        .fullscreen(SHOW_FULLSCREEN_BUTTON)
                        .build()
                addFullscreenListener(
                    object : FullscreenListener {
                        override fun onEnterFullscreen(
                            fullscreenView: View,
                            exitFullscreen: () -> Unit,
                        ) {
                            currentOnEnterFullscreen(FullscreenVideo(fullscreenView, exitFullscreen))
                        }

                        override fun onExitFullscreen() {
                            currentOnExitFullscreen()
                        }
                    },
                )
                initialize(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            controller.onReady(youTubePlayer, currentVideoId.value, currentStartPosition)
                        }

                        override fun onVideoDuration(
                            youTubePlayer: YouTubePlayer,
                            duration: Float,
                        ) {
                            controller.onVideoDuration(duration)
                        }

                        override fun onCurrentSecond(
                            youTubePlayer: YouTubePlayer,
                            second: Float,
                        ) {
                            controller.positionAt(second)?.let { (id, position) -> currentOnProgress(id, position) }
                        }

                        override fun onStateChange(
                            youTubePlayer: YouTubePlayer,
                            state: PlayerConstants.PlayerState,
                        ) {
                            controller.onStateChange(state)
                            currentOnPlayingChange(state == PlayerConstants.PlayerState.PLAYING)
                            when (state) {
                                PlayerConstants.PlayerState.PLAYING -> {
                                    currentOnPlaybackStarted()
                                }

                                PlayerConstants.PlayerState.PAUSED -> {
                                    currentOnPaused()
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
        update = { controller.load(videoId.value, startPosition) },
        onRelease = { playerView ->
            lifecycle.removeObserver(playerView)
            playerView.release()
        },
    )
}
