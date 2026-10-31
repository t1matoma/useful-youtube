package dev.satoshi.usefulyoutube.core.data.mapper

import dev.satoshi.usefulyoutube.core.database.entity.UnclassifiedVideo
import dev.satoshi.usefulyoutube.core.database.entity.VideoDetailsUpdate
import dev.satoshi.usefulyoutube.core.database.entity.VideoEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoWithChannel
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.network.model.RssEntryDto
import dev.satoshi.usefulyoutube.core.network.model.VideoItemDto
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val THUMBNAIL_PREFERENCE = listOf("maxres", "standard", "high", "medium", "default")

internal fun VideoWithChannel.toVideo() =
    Video(
        id = VideoId(video.id),
        channelId = ChannelId(video.channelId),
        channelTitle = channelTitle,
        title = video.title,
        description = video.description,
        publishedAt = Instant.fromEpochMilliseconds(video.publishedAtEpochMillis),
        duration = video.durationSeconds?.seconds,
        thumbnailUrl = video.thumbnailUrl,
        contentKind = video.contentKind,
    )

/** Returns `null` when the entry has an unparsable publication date. */
internal fun RssEntryDto.toVideoEntity(): VideoEntity? {
    val publishedAt = Instant.parseOrNull(published) ?: return null
    return VideoEntity(
        id = videoId,
        channelId = channelId,
        title = title,
        description = mediaGroup?.description.orEmpty(),
        publishedAtEpochMillis = publishedAt.toEpochMilliseconds(),
        durationSeconds = null,
        thumbnailUrl = mediaGroup?.thumbnail?.url ?: defaultThumbnailUrl(videoId),
        contentKind = VideoContentKind.UNCLASSIFIED,
        detailsFetched = false,
    )
}

internal fun VideoItemDto.toDetailsUpdate() =
    VideoDetailsUpdate(
        id = id,
        title = snippet.title,
        description = snippet.description,
        durationSeconds = contentDetails?.duration?.let(::parseIsoDuration)?.inWholeSeconds,
        thumbnailUrl =
            THUMBNAIL_PREFERENCE.firstNotNullOfOrNull { snippet.thumbnails[it]?.url } ?: defaultThumbnailUrl(id),
    )

internal fun UnclassifiedVideo.toShortsCandidate() =
    ShortsCandidate(id = VideoId(id), duration = durationSeconds?.seconds)

/** Parses ISO 8601 durations returned by the Data API (`PT4M13S`, `P1DT2H`, `P0D`). */
internal fun parseIsoDuration(value: String): Duration? = Duration.parseIsoStringOrNull(value)

private fun defaultThumbnailUrl(videoId: String) = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
