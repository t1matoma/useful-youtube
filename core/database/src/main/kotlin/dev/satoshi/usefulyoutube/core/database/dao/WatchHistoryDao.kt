package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.satoshi.usefulyoutube.core.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY watched_at DESC")
    abstract fun observeHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE video_id = :videoId")
    abstract fun observeEntry(videoId: String): Flow<WatchHistoryEntity?>

    /** Stores the [entry] on top and keeps only the [limit] most recently watched videos. */
    @Transaction
    open suspend fun record(
        entry: WatchHistoryEntity,
        limit: Int,
    ) {
        upsert(entry)
        trim(limit)
    }

    @Query("DELETE FROM watch_history WHERE video_id = :videoId")
    abstract suspend fun delete(videoId: String)

    @Query("DELETE FROM watch_history")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsert(entry: WatchHistoryEntity)

    @Query(
        """
        DELETE FROM watch_history WHERE video_id NOT IN
        (SELECT video_id FROM watch_history ORDER BY watched_at DESC LIMIT :limit)
        """,
    )
    protected abstract suspend fun trim(limit: Int)
}
