package dev.satoshi.usefulyoutube.core.data.testdoubles

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.model.ChannelItemDto
import dev.satoshi.usefulyoutube.core.network.model.ChannelSnippetDto
import dev.satoshi.usefulyoutube.core.network.model.RssAuthorDto
import dev.satoshi.usefulyoutube.core.network.model.RssEntryDto
import dev.satoshi.usefulyoutube.core.network.model.RssFeedDto
import dev.satoshi.usefulyoutube.core.network.model.RssMediaGroupDto
import dev.satoshi.usefulyoutube.core.network.model.RssThumbnailDto
import dev.satoshi.usefulyoutube.core.network.model.VideoContentDetailsDto
import dev.satoshi.usefulyoutube.core.network.model.VideoItemDto
import dev.satoshi.usefulyoutube.core.network.model.VideoSnippetDto

class FakeYouTubeNetworkDataSource : YouTubeNetworkDataSource {
    val feeds = mutableMapOf<String, AppResult<RssFeedDto, DataError>>()
    val videoDetails = mutableMapOf<String, VideoItemDto>()
    var videoDetailsError: DataError? = null
    val requestedDetailIds = mutableListOf<List<String>>()
    val channelsByHandle = mutableMapOf<String, ChannelItemDto>()
    var handleLookupError: DataError? = null
    val shortsUrls = mutableMapOf<String, AppResult<Boolean, DataError>>()

    override suspend fun getChannelFeed(channelId: String): AppResult<RssFeedDto, DataError> =
        feeds[channelId] ?: AppResult.Failure(DataError.NotFound)

    override suspend fun getVideoDetails(videoIds: List<String>): AppResult<List<VideoItemDto>, DataError> {
        requestedDetailIds += videoIds
        videoDetailsError?.let { return AppResult.Failure(it) }
        return AppResult.Success(videoIds.mapNotNull { videoDetails[it] })
    }

    override suspend fun getChannelByHandle(handle: String): AppResult<ChannelItemDto?, DataError> =
        handleLookupError?.let { AppResult.Failure(it) } ?: AppResult.Success(channelsByHandle[handle])

    override suspend fun isShortsUrl(videoId: String): AppResult<Boolean, DataError> =
        shortsUrls[videoId] ?: AppResult.Failure(DataError.Unknown)

    companion object {
        fun feed(
            title: String,
            vararg entries: RssEntryDto,
        ) = AppResult.Success(RssFeedDto(title = title, entries = entries.toList()))

        fun entry(
            videoId: String,
            channelId: String,
            published: String = "2026-10-30T16:00:06+00:00",
        ) = RssEntryDto(
            videoId = videoId,
            channelId = channelId,
            title = "RSS title $videoId",
            published = published,
            author = RssAuthorDto(name = "Author"),
            mediaGroup =
                RssMediaGroupDto(
                    thumbnail = RssThumbnailDto("https://i1.ytimg.com/vi/$videoId/hqdefault.jpg"),
                    description = "RSS description",
                ),
        )

        fun details(
            videoId: String,
            channelId: String,
            duration: String?,
        ) = VideoItemDto(
            id = videoId,
            snippet =
                VideoSnippetDto(
                    channelId = channelId,
                    title = "API title $videoId",
                    description = "API description",
                    publishedAt = "2026-10-30T16:00:06Z",
                ),
            contentDetails = VideoContentDetailsDto(duration = duration),
        )

        fun channel(
            id: String,
            title: String,
        ) = ChannelItemDto(id = id, snippet = ChannelSnippetDto(title = title))
    }
}
