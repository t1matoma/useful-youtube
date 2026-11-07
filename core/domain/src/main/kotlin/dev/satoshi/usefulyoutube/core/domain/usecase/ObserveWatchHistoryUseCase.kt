package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWatchHistoryUseCase
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
    ) {
        operator fun invoke(): Flow<List<WatchedVideo>> = historyRepository.observeHistory()
    }
