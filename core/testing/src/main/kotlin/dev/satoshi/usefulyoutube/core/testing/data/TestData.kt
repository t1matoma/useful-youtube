package dev.satoshi.usefulyoutube.core.testing.data

import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object TestData {
    val firstChannel = Channel(ChannelId("UCaaaaaaaaaaaaaaaaaaaaaa"), "First channel")
    val secondChannel = Channel(ChannelId("UCbbbbbbbbbbbbbbbbbbbbbb"), "Second channel")

    fun video(
        id: String = "video00001",
        channel: Channel = firstChannel,
        publishedAt: Instant = Instant.parse("2026-10-01T10:00:00Z"),
        duration: Duration? = 10.minutes,
        contentKind: VideoContentKind = VideoContentKind.REGULAR,
    ) = Video(
        id = VideoId(id),
        channelId = channel.id,
        channelTitle = channel.title,
        title = "Title of $id",
        description = "Description of $id",
        publishedAt = publishedAt,
        duration = duration,
        thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        contentKind = contentKind,
    )
}
