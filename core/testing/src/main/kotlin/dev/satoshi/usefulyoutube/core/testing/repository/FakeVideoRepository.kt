package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeVideoRepository(
    initialVideos: List<Video> = emptyList(),
) : VideoRepository {
    private val videos = MutableStateFlow(initialVideos)

    /** Videos returned by [syncChannelFeed] for each channel. */
    val remoteFeeds = mutableMapOf<ChannelId, List<Video>>()

    /** Channels for which [syncChannelFeed] fails. */
    val failingChannels = mutableMapOf<ChannelId, DataError>()

    /** When set, [fetchMissingDetails] fails with this error. */
    var detailsError: DataError? = null

    val syncedChannels = mutableListOf<ChannelId>()

    val currentVideos: List<Video>
        get() = videos.value

    override fun observeFeed(): Flow<List<Video>> =
        videos.map { list ->
            list
                .filter {
                    it.contentKind == VideoContentKind.REGULAR ||
                        it.contentKind == VideoContentKind.LIVE_OR_PREMIERE
                }.sortedByDescending { it.publishedAt }
        }

    override fun observeVideo(id: VideoId): Flow<Video?> = videos.map { list -> list.find { it.id == id } }

    override suspend fun syncChannelFeed(channelId: ChannelId): AppResult<Int, DataError> {
        syncedChannels += channelId
        failingChannels[channelId]?.let { return AppResult.Failure(it) }
        val existingIds = videos.value.map { it.id }.toSet()
        val newVideos = remoteFeeds[channelId].orEmpty().filterNot { it.id in existingIds }
        videos.update { it + newVideos }
        return AppResult.Success(newVideos.size)
    }

    override suspend fun fetchMissingDetails(): AppResult<Unit, DataError> =
        detailsError?.let { AppResult.Failure(it) } ?: AppResult.Success(Unit)

    override suspend fun getUnclassifiedVideos(): List<ShortsCandidate> =
        videos.value
            .filter { it.contentKind == VideoContentKind.UNCLASSIFIED && it.duration != null }
            .map { ShortsCandidate(it.id, it.duration) }

    override suspend fun saveContentKinds(kinds: Map<VideoId, VideoContentKind>) {
        videos.update { list -> list.map { video -> kinds[video.id]?.let { video.copy(contentKind = it) } ?: video } }
    }
}
