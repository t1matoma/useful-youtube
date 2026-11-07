package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.mapper.toPlaylist
import dev.satoshi.usefulyoutube.core.data.mapper.toSnapshot
import dev.satoshi.usefulyoutube.core.data.mapper.toVideo
import dev.satoshi.usefulyoutube.core.database.dao.PlaylistDao
import dev.satoshi.usefulyoutube.core.database.dao.PlaylistItemDao
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistEntity
import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.Playlist
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class OfflineFirstPlaylistRepository
    @Inject
    constructor(
        private val playlistDao: PlaylistDao,
        private val itemDao: PlaylistItemDao,
    ) : PlaylistRepository {
        override fun observePlaylists(): Flow<List<Playlist>> =
            playlistDao.observePlaylists().map { playlists -> playlists.map { it.toPlaylist() } }

        override fun observePlaylist(id: PlaylistId): Flow<PlaylistWithVideos?> =
            combine(playlistDao.observePlaylist(id.value), itemDao.observeItems(id.value)) { playlist, items ->
                playlist?.let { PlaylistWithVideos(it.toPlaylist(), items.map { item -> item.video.toVideo() }) }
            }

        override fun observePlaylistsContaining(videoId: VideoId): Flow<Set<PlaylistId>> =
            itemDao.observePlaylistIdsContaining(videoId.value).map { ids -> ids.map(::PlaylistId).toSet() }

        override fun observeVideo(id: VideoId): Flow<Video?> =
            itemDao.observeItem(id.value).map { it?.video?.toVideo() }

        override suspend fun createPlaylist(title: String): PlaylistId =
            PlaylistId(playlistDao.insertPlaylist(PlaylistEntity(title = title)))

        override suspend fun renamePlaylist(
            id: PlaylistId,
            title: String,
        ) = playlistDao.renameLocalPlaylist(id.value, title)

        override suspend fun deletePlaylist(id: PlaylistId) = playlistDao.deleteLocalPlaylist(id.value)

        override suspend fun addVideo(
            id: PlaylistId,
            video: Video,
        ) = itemDao.addToLocalPlaylist(id.value, video.toSnapshot())

        override suspend fun removeVideo(
            id: PlaylistId,
            videoId: VideoId,
        ) = itemDao.removeFromLocalPlaylist(id.value, videoId.value)
    }
