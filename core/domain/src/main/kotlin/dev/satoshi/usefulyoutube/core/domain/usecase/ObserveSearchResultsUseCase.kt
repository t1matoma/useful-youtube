package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import dev.satoshi.usefulyoutube.core.model.SearchResults
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSearchResultsUseCase
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
    ) {
        operator fun invoke(): Flow<SearchResults?> = searchRepository.observeResults()
    }
