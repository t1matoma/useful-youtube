package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.satoshi.usefulyoutube.core.database.entity.UnclassifiedVideo
import dev.satoshi.usefulyoutube.core.database.entity.VideoContentKindUpdate
import dev.satoshi.usefulyoutube.core.database.entity.VideoDetailsUpdate
import dev.satoshi.usefulyoutube.core.database.entity.VideoEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoWithChannel
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query(
        """
        SELECT videos.*, channels.title AS channel_title FROM videos
        INNER JOIN channels ON channels.id = videos.channel_id
        WHERE videos.content_kind IN (:kinds)
        ORDER BY videos.published_at DESC
        """,
    )
    fun observeVideos(kinds: Set<VideoContentKind>): Flow<List<VideoWithChannel>>

    @Query(
        """
        SELECT videos.*, channels.title AS channel_title FROM videos
        INNER JOIN channels ON channels.id = videos.channel_id
        WHERE videos.id = :id
        """,
    )
    fun observeVideo(id: String): Flow<VideoWithChannel?>

    /** Returns row ids; `-1` marks videos that already existed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(videos: List<VideoEntity>): List<Long>

    @Query("SELECT id FROM videos WHERE details_fetched = 0")
    suspend fun getIdsWithoutDetails(): List<String>

    @Update(entity = VideoEntity::class)
    suspend fun updateDetails(details: List<VideoDetailsUpdate>)

    @Query("SELECT id, duration_seconds FROM videos WHERE details_fetched = 1 AND content_kind = 'UNCLASSIFIED'")
    suspend fun getUnclassifiedVideos(): List<UnclassifiedVideo>

    @Update(entity = VideoEntity::class)
    suspend fun updateContentKinds(kinds: List<VideoContentKindUpdate>)
}
