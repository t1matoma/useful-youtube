package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import javax.inject.Inject

class DeleteRecentSearchQueryUseCase
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
    ) {
        suspend operator fun invoke(query: String) = searchRepository.deleteRecentQuery(query)
    }
