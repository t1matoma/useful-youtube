package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.satoshi.usefulyoutube.core.database.entity.MirroredPlaylist
import dev.satoshi.usefulyoutube.core.database.entity.MirroredPlaylistRef
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistEntity
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistItemEntity
import dev.satoshi.usefulyoutube.core.database.entity.StoredVideoKind

/** The copy of the YouTube account's playlists (`remote_id IS NOT NULL`); app playlists are never touched. */
@Dao
abstract class MirroredPlaylistDao {
    @Query(
        """
        SELECT DISTINCT video_id, content_kind FROM playlist_items
        WHERE playlist_id IN (SELECT id FROM playlists WHERE remote_id IS NOT NULL)
        """,
    )
    abstract suspend fun getStoredVideoKinds(): List<StoredVideoKind>

    /**
     * Replaces the mirror in one transaction. Playlists keep their local ids, so open screens stay valid;
     * playlists that are not in [playlists] anymore are deleted with their videos.
     */
    @Transaction
    open suspend fun replaceAll(
        playlists: List<MirroredPlaylist>,
        syncedAtEpochMillis: Long,
    ) {
        deleteMissing(playlists.map { it.remoteId })
        val existingIds = getMirroredPlaylists().associate { it.remoteId to it.id }
        playlists.forEach { playlist ->
            val id =
                existingIds[playlist.remoteId]?.also { updatePlaylist(it, playlist.title, syncedAtEpochMillis) }
                    ?: insertPlaylist(
                        PlaylistEntity(
                            title = playlist.title,
                            remoteId = playlist.remoteId,
                            syncedAtEpochMillis = syncedAtEpochMillis,
                        ),
                    )
            deleteItems(id)
            insertItems(
                playlist.videos
                    .distinctBy { it.videoId }
                    .mapIndexed { position, video -> PlaylistItemEntity(id, position, video) },
            )
        }
    }

    @Query("SELECT id, remote_id FROM playlists WHERE remote_id IS NOT NULL")
    protected abstract suspend fun getMirroredPlaylists(): List<MirroredPlaylistRef>

    @Query("DELETE FROM playlists WHERE remote_id IS NOT NULL AND remote_id NOT IN (:remoteIds)")
    protected abstract suspend fun deleteMissing(remoteIds: List<String>)

    @Insert
    protected abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET title = :title, synced_at = :syncedAtEpochMillis WHERE id = :id")
    protected abstract suspend fun updatePlaylist(
        id: Long,
        title: String,
        syncedAtEpochMillis: Long,
    )

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId")
    protected abstract suspend fun deleteItems(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertItems(items: List<PlaylistItemEntity>)
}
