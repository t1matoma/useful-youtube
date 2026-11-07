package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.Playlist
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow

/** Changing methods affect only app playlists; mirrored YouTube playlists are read-only. */
interface PlaylistRepository {
    /** App and YouTube playlists sorted by title. */
    fun observePlaylists(): Flow<List<Playlist>>

    fun observePlaylist(id: PlaylistId): Flow<PlaylistWithVideos?>

    fun observePlaylistsContaining(videoId: VideoId): Flow<Set<PlaylistId>>

    /** A stored copy of the video from any playlist. */
    fun observeVideo(id: VideoId): Flow<Video?>

    suspend fun createPlaylist(title: String): PlaylistId

    suspend fun renamePlaylist(
        id: PlaylistId,
        title: String,
    )

    suspend fun deletePlaylist(id: PlaylistId)

    /** Appends the video to the end; a video already in the playlist is not added again. */
    suspend fun addVideo(
        id: PlaylistId,
        video: Video,
    )

    suspend fun removeVideo(
        id: PlaylistId,
        videoId: VideoId,
    )
}
