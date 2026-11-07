package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import javax.inject.Inject

/**
 * Searches YouTube for videos on an explicit request of the user, removes Shorts and stores the first page
 * in place of the previous search. The query is remembered as a recent one even if the search fails.
 */
class SearchVideosUseCase
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
        private val classifyVideo: ClassifyVideoUseCase,
    ) {
        suspend operator fun invoke(
            query: String,
            order: SearchOrder,
        ): AppResult<Unit, DataError> {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) return AppResult.Success(Unit)
            searchRepository.saveRecentQuery(normalizedQuery)
            return when (val page = searchRepository.fetchPage(normalizedQuery, order, pageToken = null)) {
                is AppResult.Failure -> {
                    page
                }

                is AppResult.Success -> {
                    searchRepository.saveFirstPage(
                        session = SearchSession(normalizedQuery, order, page.value.nextPageToken),
                        videos = classifyVideo.keepWatchable(page.value.videos),
                    )
                    AppResult.Success(Unit)
                }
            }
        }
    }
