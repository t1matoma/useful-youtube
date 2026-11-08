package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.VideoId
import javax.inject.Inject

/** The position is kept with the video's history entry, so the video must have been recorded first. */
class SavePlaybackPositionUseCase
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
    ) {
        suspend operator fun invoke(
            id: VideoId,
            position: PlaybackPosition,
        ) = historyRepository.savePlaybackPosition(id, position)
    }
