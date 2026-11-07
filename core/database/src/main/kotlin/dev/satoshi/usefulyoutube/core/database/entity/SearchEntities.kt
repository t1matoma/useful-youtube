package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.VideoContentKind

/**
 * A video found by the last search. Search results come from any channel, so unlike [VideoEntity] they
 * do not reference the subscriptions.
 */
@Entity(tableName = "search_results", indices = [Index("position")])
data class SearchResultEntity(
    @PrimaryKey @ColumnInfo(name = "video_id") val videoId: String,
    val position: Int,
    @ColumnInfo(name = "channel_id") val channelId: String,
    @ColumnInfo(name = "channel_title") val channelTitle: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "published_at") val publishedAtEpochMillis: Long,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    @ColumnInfo(name = "content_kind") val contentKind: VideoContentKind,
)

/** The only row describes the last search; [nextPageToken] is `null` when there are no more pages. */
@Entity(tableName = "search_session")
data class SearchSessionEntity(
    @ColumnInfo(name = "query_text") val query: String,
    @ColumnInfo(name = "sort_order") val order: SearchOrder,
    @ColumnInfo(name = "next_page_token") val nextPageToken: String?,
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

/** A query the user searched for; a larger [usedOrder] means a more recent use. */
@Entity(tableName = "recent_search_queries")
data class RecentSearchQueryEntity(
    @PrimaryKey @ColumnInfo(name = "query_text") val query: String,
    @ColumnInfo(name = "used_order") val usedOrder: Long,
)
