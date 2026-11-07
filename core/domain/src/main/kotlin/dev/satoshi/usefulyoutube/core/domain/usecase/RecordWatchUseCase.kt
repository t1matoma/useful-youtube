package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.Video
import javax.inject.Inject

class RecordWatchUseCase
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
    ) {
        suspend operator fun invoke(video: Video) = historyRepository.recordWatched(video)
    }
