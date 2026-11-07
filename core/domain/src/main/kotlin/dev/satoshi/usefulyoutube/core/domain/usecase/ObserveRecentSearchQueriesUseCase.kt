package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecentSearchQueriesUseCase
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
    ) {
        operator fun invoke(): Flow<List<String>> = searchRepository.observeRecentQueries()
    }
