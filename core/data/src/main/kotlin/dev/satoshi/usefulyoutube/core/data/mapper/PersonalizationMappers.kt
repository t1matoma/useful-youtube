package dev.satoshi.usefulyoutube.core.data.mapper

import dev.satoshi.usefulyoutube.core.database.entity.DiscoveredVideoEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoRatingEntity
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.network.VideoRatingNetworkDataSource

internal fun VideoRatingEntity.toRatedVideo() = RatedVideo(VideoId(videoId), ChannelId(channelId), rating)

/** `unspecified` and unknown values mean that the video is not rated. */
internal fun String.toVideoRating(): VideoRating =
    when (this) {
        VideoRatingNetworkDataSource.RATING_LIKE -> VideoRating.LIKE
        VideoRatingNetworkDataSource.RATING_DISLIKE -> VideoRating.DISLIKE
        else -> VideoRating.NONE
    }

internal fun VideoRating.toApiValue(): String =
    when (this) {
        VideoRating.LIKE -> VideoRatingNetworkDataSource.RATING_LIKE
        VideoRating.DISLIKE -> VideoRatingNetworkDataSource.RATING_DISLIKE
        VideoRating.NONE -> VideoRatingNetworkDataSource.RATING_NONE
    }

internal fun DiscoveredVideoEntity.toDiscoveredVideo() =
    DiscoveredVideo(video = video.toVideo(), suggestedBy = suggestedBy?.let(::ChannelId))

internal fun DiscoveredVideo.toEntity() =
    DiscoveredVideoEntity(video = video.toSnapshot(), suggestedBy = suggestedBy?.value)
