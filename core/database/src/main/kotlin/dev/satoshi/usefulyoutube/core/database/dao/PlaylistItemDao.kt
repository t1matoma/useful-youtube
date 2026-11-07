package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistItemEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistItemDao {
    @Query("SELECT * FROM playlist_items WHERE playlist_id = :playlistId ORDER BY position")
    abstract fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    /** Any stored copy of the video, so the player can open videos of playlists. */
    @Query("SELECT * FROM playlist_items WHERE video_id = :videoId LIMIT 1")
    abstract fun observeItem(videoId: String): Flow<PlaylistItemEntity?>

    @Query("SELECT playlist_id FROM playlist_items WHERE video_id = :videoId")
    abstract fun observePlaylistIdsContaining(videoId: String): Flow<List<Long>>

    /** Appends the [video] to an app playlist; does nothing if it is already there. */
    @Transaction
    open suspend fun addToLocalPlaylist(
        playlistId: Long,
        video: VideoSnapshot,
    ) {
        if (!isLocalPlaylist(playlistId)) return
        insertOrIgnore(PlaylistItemEntity(playlistId, position = getLastPosition(playlistId) + 1, video = video))
    }

    @Query(
        """
        DELETE FROM playlist_items WHERE playlist_id = :playlistId AND video_id = :videoId
        AND playlist_id IN (SELECT id FROM playlists WHERE remote_id IS NULL)
        """,
    )
    abstract suspend fun removeFromLocalPlaylist(
        playlistId: Long,
        videoId: String,
    )

    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE id = :playlistId AND remote_id IS NULL)")
    protected abstract suspend fun isLocalPlaylist(playlistId: Long): Boolean

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_items WHERE playlist_id = :playlistId")
    protected abstract suspend fun getLastPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertOrIgnore(item: PlaylistItemEntity)
}
