package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.VideoId
import javax.inject.Inject

class RemoveFromHistoryUseCase
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
    ) {
        suspend operator fun invoke(id: VideoId) = historyRepository.remove(id)
    }
