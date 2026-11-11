package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.recommendations.RecommendationRanker
import dev.satoshi.usefulyoutube.core.domain.recommendations.RecommendationSignals
import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.domain.repository.RecommendationRepository
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Videos picked for the user on the device: unwatched videos of the subscriptions and of the channels found by
 * [RefreshRecommendationsUseCase], ordered by [RecommendationRanker]. Nothing is loaded here.
 */
class ObserveRecommendationsUseCase
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
        private val recommendationRepository: RecommendationRepository,
        private val historyRepository: HistoryRepository,
        private val ratingRepository: VideoRatingRepository,
        private val ranker: RecommendationRanker,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<List<Video>> =
            combine(
                videoRepository.observeFeed(),
                recommendationRepository.observeDiscoveredVideos(),
                historyRepository.observeHistory(),
                ratingRepository.observeRatedVideos(),
            ) { feed, discovered, history, ratedVideos ->
                ranker.rank(RecommendationSignals(feed, discovered, history, ratedVideos), now = clock.now())
            }
    }
