package dev.satoshi.usefulyoutube.core.data.mapper

import dev.satoshi.usefulyoutube.core.database.entity.PlaylistSummary
import dev.satoshi.usefulyoutube.core.database.entity.VideoSnapshot
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.Playlist
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal fun PlaylistSummary.toPlaylist() =
    Playlist(
        id = PlaylistId(playlist.id),
        title = playlist.title,
        source = if (playlist.remoteId == null) PlaylistSource.LOCAL else PlaylistSource.YOUTUBE,
        videoCount = videoCount,
        thumbnailUrl = thumbnailUrl,
    )

internal fun Video.toSnapshot() =
    VideoSnapshot(
        videoId = id.value,
        channelId = channelId.value,
        channelTitle = channelTitle,
        title = title,
        description = description,
        publishedAtEpochMillis = publishedAt.toEpochMilliseconds(),
        durationSeconds = duration?.inWholeSeconds,
        thumbnailUrl = thumbnailUrl,
        contentKind = contentKind,
    )

internal fun VideoSnapshot.toVideo() =
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
