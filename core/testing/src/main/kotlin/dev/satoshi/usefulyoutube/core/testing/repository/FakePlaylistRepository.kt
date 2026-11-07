package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.Playlist
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private data class StoredPlaylist(
    val id: PlaylistId,
    val title: String,
    val source: PlaylistSource,
    val videos: List<Video>,
) {
    fun toPlaylist() = Playlist(id, title, source, videos.size, videos.firstOrNull()?.thumbnailUrl)
}

/** Changes an app playlist; mirrored YouTube playlists stay as they are, like in the real repository. */
private fun MutableStateFlow<List<StoredPlaylist>>.updateLocal(
    id: PlaylistId,
    transform: (StoredPlaylist) -> StoredPlaylist,
) = update { list -> list.map { if (it.id == id && it.source == PlaylistSource.LOCAL) transform(it) else it } }

class FakePlaylistRepository : PlaylistRepository {
    private val playlists = MutableStateFlow(emptyList<StoredPlaylist>())
    private var nextId = 1L

    val currentPlaylists: List<PlaylistWithVideos>
        get() = playlists.value.map { PlaylistWithVideos(it.toPlaylist(), it.videos) }

    /** Adds a playlist directly, for example a mirrored YouTube playlist; returns its id. */
    fun givenPlaylist(
        title: String,
        source: PlaylistSource = PlaylistSource.LOCAL,
        videos: List<Video> = emptyList(),
    ): PlaylistId {
        val id = PlaylistId(nextId++)
        playlists.update { it + StoredPlaylist(id, title, source, videos) }
        return id
    }

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlists.map { list -> list.sortedBy { it.title.lowercase() }.map { it.toPlaylist() } }

    override fun observePlaylist(id: PlaylistId): Flow<PlaylistWithVideos?> =
        playlists.map { list ->
            list.find { it.id == id }?.let { PlaylistWithVideos(it.toPlaylist(), it.videos) }
        }

    override fun observePlaylistsContaining(videoId: VideoId): Flow<Set<PlaylistId>> =
        playlists.map { list -> list.filter { stored -> stored.videos.any { it.id == videoId } }.map { it.id }.toSet() }

    override fun observeVideo(id: VideoId): Flow<Video?> =
        playlists.map { list -> list.flatMap { it.videos }.find { it.id == id } }

    override suspend fun createPlaylist(title: String): PlaylistId = givenPlaylist(title)

    override suspend fun renamePlaylist(
        id: PlaylistId,
        title: String,
    ) = playlists.updateLocal(id) { it.copy(title = title) }

    override suspend fun deletePlaylist(id: PlaylistId) {
        playlists.update { list -> list.filterNot { it.id == id && it.source == PlaylistSource.LOCAL } }
    }

    override suspend fun addVideo(
        id: PlaylistId,
        video: Video,
    ) = playlists.updateLocal(id) { stored ->
        if (stored.videos.any { it.id == video.id }) stored else stored.copy(videos = stored.videos + video)
    }

    override suspend fun removeVideo(
        id: PlaylistId,
        videoId: VideoId,
    ) = playlists.updateLocal(id) { stored -> stored.copy(videos = stored.videos.filterNot { it.id == videoId }) }
}
