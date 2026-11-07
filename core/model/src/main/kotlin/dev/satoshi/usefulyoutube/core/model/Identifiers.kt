package dev.satoshi.usefulyoutube.core.model

@JvmInline
value class ChannelId(
    val value: String,
)

@JvmInline
value class VideoId(
    val value: String,
)

/** Local id of a playlist in the database, for app playlists and mirrored YouTube playlists alike. */
@JvmInline
value class PlaylistId(
    val value: Long,
)
