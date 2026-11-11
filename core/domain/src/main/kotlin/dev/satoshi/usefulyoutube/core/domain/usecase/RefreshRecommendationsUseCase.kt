package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.recommendations.channelAffinities
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.domain.repository.RecommendationRepository
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Looks for videos outside the subscriptions, using only what the device knows about the user:
 *
 * 1. channels the user watched or liked without subscribing to them;
 * 2. channels featured by the [MAXIMUM_FAVORITE_CHANNELS] channels the user likes most.
 *
 * The latest videos of at most [MAXIMUM_DISCOVERY_CHANNELS] such channels are loaded from their RSS feeds,
 * classified like the feed and stored; videos of channels that left the list are forgotten. Without a watch
 * history or likes there is nothing to look for and no request is made.
 */
class RefreshRecommendationsUseCase
    @Inject
    constructor(
        private val recommendationRepository: RecommendationRepository,
        private val channelRepository: ChannelRepository,
        private val historyRepository: HistoryRepository,
        private val ratingRepository: VideoRatingRepository,
        private val classifyVideo: ClassifyVideoUseCase,
    ) {
        suspend operator fun invoke(): AppResult<Unit, DataError> {
            val suggestions =
                when (val result = findChannels()) {
                    is AppResult.Failure -> return result
                    is AppResult.Success -> result.value
                }
            val videos =
                when (val result = recommendationRepository.fetchNewVideos(suggestions.keys.toList())) {
                    is AppResult.Failure -> return result
                    is AppResult.Success -> result.value
                }
            val kinds = classifyVideo.classifyAll(videos.map { ShortsCandidate(it.id, it.duration) })
            // Videos that could not be classified are not stored, so the next refresh tries again.
            val classified =
                videos.mapNotNull { video ->
                    kinds[video.id]
                        ?.takeIf { it != VideoContentKind.UNCLASSIFIED }
                        ?.let { DiscoveredVideo(video.copy(contentKind = it), suggestions[video.channelId]) }
                }
            recommendationRepository.saveDiscoveredVideos(classified, suggestions.keys)
            return AppResult.Success(Unit)
        }

        /** Channels to look into, most interesting first, with the favorite channel that features each. */
        private suspend fun findChannels(): AppResult<Map<ChannelId, ChannelId?>, DataError> {
            val subscribed = channelRepository.getChannels().map { it.id }.toSet()
            val favorites =
                channelAffinities(
                    historyRepository.observeHistory().first(),
                    ratingRepository.observeRatedVideos().first(),
                ).filterValues { it > 0 }
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            val suggestions = linkedMapOf<ChannelId, ChannelId?>()
            favorites.filterNot { it in subscribed }.forEach { suggestions[it] = null }
            for (favorite in favorites.take(MAXIMUM_FAVORITE_CHANNELS)) {
                when (val featured = recommendationRepository.getFeaturedChannels(favorite)) {
                    is AppResult.Failure -> return featured
                    is AppResult.Success -> featured.value.forEach { suggestions.putIfAbsent(it, favorite) }
                }
            }
            return AppResult.Success(
                suggestions
                    .filterKeys { it !in subscribed }
                    .entries
                    .take(MAXIMUM_DISCOVERY_CHANNELS)
                    .associate { it.key to it.value },
            )
        }

        private companion object {
            const val MAXIMUM_FAVORITE_CHANNELS = 5
            const val MAXIMUM_DISCOVERY_CHANNELS = 20
        }
    }
