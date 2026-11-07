package dev.satoshi.usefulyoutube.core.data.mapper

import dev.satoshi.usefulyoutube.core.database.entity.SearchResultEntity
import dev.satoshi.usefulyoutube.core.database.entity.SearchSessionEntity
import dev.satoshi.usefulyoutube.core.domain.repository.SearchSession
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.network.model.VideoItemDto
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Returns `null` when the publication date cannot be parsed. */
internal fun VideoItemDto.toFoundVideo(): Video? {
    val publishedAt = Instant.parseOrNull(snippet.publishedAt) ?: return null
    return Video(
        id = VideoId(id),
        channelId = ChannelId(snippet.channelId),
        channelTitle = snippet.channelTitle,
        title = snippet.title,
        description = snippet.description,
        publishedAt = publishedAt,
        duration = contentDetails?.duration?.let(::parseIsoDuration),
        thumbnailUrl = bestThumbnailUrl(),
        contentKind = VideoContentKind.UNCLASSIFIED,
    )
}

/** [SearchResultEntity.position] is assigned by the DAO. */
internal fun Video.toSearchResultEntity() =
    SearchResultEntity(
        videoId = id.value,
        position = 0,
        channelId = channelId.value,
        channelTitle = channelTitle,
        title = title,
        description = description,
        publishedAtEpochMillis = publishedAt.toEpochMilliseconds(),
        durationSeconds = duration?.inWholeSeconds,
        thumbnailUrl = thumbnailUrl,
        contentKind = contentKind,
    )

internal fun SearchResultEntity.toVideo() =
    Video(
        id = VideoId(videoId),
        channelId = ChannelId(channelId),
        channelTitle = channelTitle,
        title = title,
        description = description,
        publishedAt = Instant.fromEpochMilliseconds(publishedAtEpochMillis),
        duration = durationSeconds?.seconds,
        thumbnailUrl = thumbnailUrl,
        contentKind = contentKind,
    )

internal fun SearchSessionEntity.toSearchSession() = SearchSession(query, order, nextPageToken)

internal fun SearchSession.toEntity() = SearchSessionEntity(query = query, order = order, nextPageToken = nextPageToken)
