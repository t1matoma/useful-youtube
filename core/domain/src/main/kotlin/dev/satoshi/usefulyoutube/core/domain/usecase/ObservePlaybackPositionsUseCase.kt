package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Where watched videos stopped, for the progress bars of the video lists. */
class ObservePlaybackPositionsUseCase
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
    ) {
        operator fun invoke(): Flow<Map<VideoId, PlaybackPosition>> = historyRepository.observePlaybackPositions()
    }
