package dev.satoshi.usefulyoutube.core.model

enum class PlaylistSource {
    /** Created in the app; stored only on the device and editable. */
    LOCAL,

    /** A read-only copy of a playlist of the YouTube account. */
    YOUTUBE,
}

data class Playlist(
    val id: PlaylistId,
    val title: String,
    val source: PlaylistSource,
    val videoCount: Int,
    /** Thumbnail of the first video; `null` for an empty playlist. */
    val thumbnailUrl: String?,
)

data class PlaylistWithVideos(
    val playlist: Playlist,
    val videos: List<Video>,
)
