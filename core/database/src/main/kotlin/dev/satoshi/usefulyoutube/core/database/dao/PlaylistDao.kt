package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistEntity
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistSummary
import kotlinx.coroutines.flow.Flow

/** Changes are limited to app playlists (`remote_id IS NULL`); mirrored YouTube playlists are read-only. */
@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT playlists.*,
            (SELECT COUNT(*) FROM playlist_items WHERE playlist_id = playlists.id) AS video_count,
            (SELECT thumbnail_url FROM playlist_items WHERE playlist_id = playlists.id
                ORDER BY position LIMIT 1) AS thumbnail_url
        FROM playlists ORDER BY title COLLATE NOCASE
        """,
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query(
        """
        SELECT playlists.*,
            (SELECT COUNT(*) FROM playlist_items WHERE playlist_id = playlists.id) AS video_count,
            (SELECT thumbnail_url FROM playlist_items WHERE playlist_id = playlists.id
                ORDER BY position LIMIT 1) AS thumbnail_url
        FROM playlists WHERE id = :id
        """,
    )
    fun observePlaylist(id: Long): Flow<PlaylistSummary?>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET title = :title WHERE id = :id AND remote_id IS NULL")
    suspend fun renameLocalPlaylist(
        id: Long,
        title: String,
    )

    @Query("DELETE FROM playlists WHERE id = :id AND remote_id IS NULL")
    suspend fun deleteLocalPlaylist(id: Long)
}
