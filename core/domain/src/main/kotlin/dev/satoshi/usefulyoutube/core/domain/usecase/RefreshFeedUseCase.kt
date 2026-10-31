package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.RefreshSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

/**
 * Updates the local feed: loads RSS feeds of all subscriptions in parallel, fetches missing video
 * details and classifies new videos. A failure of a single channel does not stop the refresh.
 */
class RefreshFeedUseCase
    @Inject
    constructor(
        private val channelRepository: ChannelRepository,
        private val videoRepository: VideoRepository,
        private val classifyVideo: ClassifyVideoUseCase,
    ) {
        suspend operator fun invoke(): AppResult<RefreshSummary, DataError> {
            val channelIds = channelRepository.getChannels().map { it.id }
            val syncResults = syncFeeds(channelIds)
            val failures = syncResults.filterValues { it is AppResult.Failure }
            if (channelIds.isNotEmpty() && failures.size == channelIds.size) {
                return failures.values.first() as AppResult.Failure
            }

            val detailsResult = videoRepository.fetchMissingDetails()
            if (detailsResult is AppResult.Failure) return detailsResult

            classifyNewVideos()

            val newVideoCount = syncResults.values.sumOf { (it as? AppResult.Success)?.value ?: 0 }
            return AppResult.Success(
                RefreshSummary(newVideoCount = newVideoCount, failedChannels = failures.keys.toList()),
            )
        }

        private suspend fun syncFeeds(channelIds: List<ChannelId>): Map<ChannelId, AppResult<Int, DataError>> =
            coroutineScope {
                val semaphore = Semaphore(MAXIMUM_CONCURRENT_REQUESTS)
                channelIds
                    .map { channelId ->
                        async {
                            semaphore.withPermit { channelId to videoRepository.syncChannelFeed(channelId) }
                        }
                    }.awaitAll()
                    .toMap()
            }

        private suspend fun classifyNewVideos() =
            coroutineScope {
                val semaphore = Semaphore(MAXIMUM_CONCURRENT_REQUESTS)
                val kinds =
                    videoRepository
                        .getUnclassifiedVideos()
                        .map { candidate ->
                            async { semaphore.withPermit { candidate.id to classifyVideo(candidate) } }
                        }.awaitAll()
                        .toMap()
                videoRepository.saveContentKinds(kinds)
            }

        private companion object {
            const val MAXIMUM_CONCURRENT_REQUESTS = 8
        }
    }
