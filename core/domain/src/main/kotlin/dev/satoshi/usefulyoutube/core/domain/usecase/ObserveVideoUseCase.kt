package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.HistoryRepository
import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.domain.repository.RecommendationRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * A video of the feed or, for videos of other channels, of the last search, a playlist, the history or the
 * recommendations.
 */
class ObserveVideoUseCase
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
        private val searchRepository: SearchRepository,
        private val playlistRepository: PlaylistRepository,
        private val historyRepository: HistoryRepository,
        private val recommendationRepository: RecommendationRepository,
    ) {
        operator fun invoke(id: VideoId): Flow<Video?> =
            combine(
                videoRepository.observeVideo(id),
                searchRepository.observeVideo(id),
                playlistRepository.observeVideo(id),
                historyRepository.observeVideo(id),
                recommendationRepository.observeVideo(id),
            ) { feedVideo, foundVideo, savedVideo, watchedVideo, discoveredVideo ->
                feedVideo ?: foundVideo ?: savedVideo ?: watchedVideo ?: discoveredVideo
            }
    }
