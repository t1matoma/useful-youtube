package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import javax.inject.Inject

/** Loads the next page of the stored search; does nothing when there are no more pages. */
class LoadMoreSearchResultsUseCase
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
        private val classifyVideo: ClassifyVideoUseCase,
    ) {
        suspend operator fun invoke(): AppResult<Unit, DataError> {
            val session = searchRepository.getSession()
            val pageToken = session?.nextPageToken ?: return AppResult.Success(Unit)
            return when (val page = searchRepository.fetchPage(session.query, session.order, pageToken)) {
                is AppResult.Failure -> {
                    page
                }

                is AppResult.Success -> {
                    searchRepository.saveNextPage(
                        nextPageToken = page.value.nextPageToken,
                        videos = classifyVideo.keepWatchable(page.value.videos),
                    )
                    AppResult.Success(Unit)
                }
            }
        }
    }
