package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.satoshi.usefulyoutube.core.database.entity.RecentSearchQueryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecentSearchQueryDao {
    @Query("SELECT query_text FROM recent_search_queries ORDER BY used_order DESC")
    abstract fun observeRecentQueries(): Flow<List<String>>

    /** Moves the [query] to the top of the recent queries and keeps only the [limit] most recent ones. */
    @Transaction
    open suspend fun saveRecentQuery(
        query: String,
        limit: Int,
    ) {
        upsertRecentQuery(RecentSearchQueryEntity(query = query, usedOrder = getLastUsedOrder() + 1))
        trimRecentQueries(limit)
    }

    @Query("DELETE FROM recent_search_queries WHERE query_text = :query")
    abstract suspend fun deleteRecentQuery(query: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertRecentQuery(query: RecentSearchQueryEntity)

    @Query("SELECT COALESCE(MAX(used_order), 0) FROM recent_search_queries")
    protected abstract suspend fun getLastUsedOrder(): Long

    @Query(
        """
        DELETE FROM recent_search_queries WHERE query_text NOT IN
        (SELECT query_text FROM recent_search_queries ORDER BY used_order DESC LIMIT :limit)
        """,
    )
    protected abstract suspend fun trimRecentQueries(limit: Int)
}
