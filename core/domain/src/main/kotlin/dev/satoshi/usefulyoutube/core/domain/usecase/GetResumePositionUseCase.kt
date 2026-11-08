package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.VideoId
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Where the playback of a video continues: where it stopped last time, or at the start for a video that was
 * watched only briefly or almost to the end.
 */
class GetResumePositionUseCase
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
    ) {
        suspend operator fun invoke(id: VideoId): Duration {
            val saved = historyRepository.getPlaybackPosition(id) ?: return Duration.ZERO
            val isBrief = saved.position < MINIMUM_WATCHED
            return if (isBrief || saved.isFinished) Duration.ZERO else saved.position
        }

        companion object {
            val MINIMUM_WATCHED = 5.seconds
        }
    }
