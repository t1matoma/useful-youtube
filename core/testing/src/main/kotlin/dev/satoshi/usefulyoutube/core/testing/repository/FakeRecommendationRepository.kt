package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.RecommendationRepository
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeRecommendationRepository(
    initialVideos: List<DiscoveredVideo> = emptyList(),
) : RecommendationRepository {
    private val stored = MutableStateFlow(initialVideos)

    /** Channels featured by each channel. */
    val featuredChannels = mutableMapOf<ChannelId, List<ChannelId>>()

    /** Videos that [fetchNewVideos] finds for each channel. */
    val remoteVideos = mutableMapOf<ChannelId, List<Video>>()

    /** When set, [getFeaturedChannels] fails with this error. */
    var featuredError: DataError? = null

    /** When set, [fetchNewVideos] fails with this error. */
    var videosError: DataError? = null

    val featuredRequests = mutableListOf<ChannelId>()
    val videoRequests = mutableListOf<List<ChannelId>>()

    /** Everything stored, Shorts included. */
    val storedVideos: List<DiscoveredVideo>
        get() = stored.value

    override fun observeDiscoveredVideos(): Flow<List<DiscoveredVideo>> =
        stored.map { list -> list.filter { it.video.contentKind != VideoContentKind.SHORT } }

    override fun observeVideo(id: VideoId): Flow<Video?> = stored.map { list -> list.find { it.video.id == id }?.video }

    override suspend fun getFeaturedChannels(channelId: ChannelId): AppResult<List<ChannelId>, DataError> {
        featuredRequests += channelId
        featuredError?.let { return AppResult.Failure(it) }
        return AppResult.Success(featuredChannels[channelId].orEmpty())
    }

    override suspend fun fetchNewVideos(channelIds: List<ChannelId>): AppResult<List<Video>, DataError> {
        videoRequests += channelIds
        videosError?.let { return AppResult.Failure(it) }
        val storedIds = stored.value.map { it.video.id }.toSet()
        return AppResult.Success(channelIds.flatMap { remoteVideos[it].orEmpty() }.filterNot { it.id in storedIds })
    }

    override suspend fun saveDiscoveredVideos(
        videos: List<DiscoveredVideo>,
        channelIds: Set<ChannelId>,
    ) {
        stored.update { list -> (list + videos).filter { it.video.channelId in channelIds } }
    }
}
