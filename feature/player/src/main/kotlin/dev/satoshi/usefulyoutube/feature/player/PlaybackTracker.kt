package dev.satoshi.usefulyoutube.feature.player

import dev.satoshi.usefulyoutube.core.domain.usecase.GetResumePositionUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RecordWatchUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.SavePlaybackPositionUseCase
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Puts the played video into the history and saves where its playback stopped: every [SAVE_INTERVAL] of
 * playback and when it is paused, ended or left, so little is lost when the system kills the app. Live streams
 * have no position. One tracker belongs to one player screen.
 */
class PlaybackTracker
    @Inject
    constructor(
        private val recordWatch: RecordWatchUseCase,
        private val getResumePosition: GetResumePositionUseCase,
        private val savePlaybackPosition: SavePlaybackPositionUseCase,
    ) {
        /** The position is stored with the history entry, so writes run in order: recording comes first. */
        private val writes = Mutex()
        private var recordedVideoId: VideoId? = null
        private var trackedVideoId: VideoId? = null
        private var latestPosition: PlaybackPosition? = null
        private var savedPosition: Duration = Duration.ZERO

        /** Where the video starts; from now on its progress is tracked. */
        suspend fun startPosition(id: VideoId): Duration {
            val start = getResumePosition(id)
            trackedVideoId = id
            latestPosition = null
            savedPosition = start
            return start
        }

        /** The first start puts the video into the history; pausing and resuming does not record it again. */
        suspend fun onStarted(video: Video) {
            if (video.id == recordedVideoId) return
            recordedVideoId = video.id
            writes.withLock { recordWatch(video) }
        }

        suspend fun onProgress(
            video: Video,
            position: PlaybackPosition,
        ) {
            if (video.id != trackedVideoId || video.contentKind == VideoContentKind.LIVE_OR_PREMIERE) return
            latestPosition = position
            if ((position.position - savedPosition).absoluteValue >= SAVE_INTERVAL) save(video.id, position)
        }

        /** Paused, left or skipped: saves where the video stopped. */
        suspend fun onStopped() {
            val id = trackedVideoId ?: return
            val position = latestPosition ?: return
            if (position.position != savedPosition) save(id, position)
        }

        /** A finished video shows a full progress bar and starts from the beginning next time. */
        suspend fun onEnded() {
            latestPosition = latestPosition?.let { it.copy(position = it.duration) }
            onStopped()
        }

        private suspend fun save(
            id: VideoId,
            position: PlaybackPosition,
        ) {
            savedPosition = position.position
            writes.withLock { savePlaybackPosition(id, position) }
        }

        private companion object {
            val SAVE_INTERVAL = 5.seconds
        }
    }
