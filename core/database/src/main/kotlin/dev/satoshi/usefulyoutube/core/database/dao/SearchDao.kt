package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.satoshi.usefulyoutube.core.database.entity.SearchResultEntity
import dev.satoshi.usefulyoutube.core.database.entity.SearchSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SearchDao {
    @Query("SELECT * FROM search_session LIMIT 1")
    abstract fun observeSession(): Flow<SearchSessionEntity?>

    @Query("SELECT * FROM search_results ORDER BY position")
    abstract fun observeResults(): Flow<List<SearchResultEntity>>

    @Query("SELECT * FROM search_results WHERE video_id = :videoId")
    abstract fun observeResult(videoId: String): Flow<SearchResultEntity?>

    /** Replaces the last search; positions of [results] start at zero. */
    @Transaction
    open suspend fun replaceSearch(
        session: SearchSessionEntity,
        results: List<SearchResultEntity>,
    ) {
        deleteResults()
        upsertSession(session)
        insertOrIgnoreResults(results.mapIndexed { index, result -> result.copy(position = index) })
    }

    /** Adds the next page after the stored results; videos that are already stored are skipped. */
    @Transaction
    open suspend fun appendPage(
        nextPageToken: String?,
        results: List<SearchResultEntity>,
    ) {
        val start = getLastPosition() + 1
        insertOrIgnoreResults(results.mapIndexed { index, result -> result.copy(position = start + index) })
        updateNextPageToken(nextPageToken)
    }

    @Query("DELETE FROM search_results")
    protected abstract suspend fun deleteResults()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertSession(session: SearchSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertOrIgnoreResults(results: List<SearchResultEntity>)

    @Query("SELECT COALESCE(MAX(position), -1) FROM search_results")
    protected abstract suspend fun getLastPosition(): Int

    @Query("UPDATE search_session SET next_page_token = :nextPageToken")
    protected abstract suspend fun updateNextPageToken(nextPageToken: String?)
}
