package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.satoshi.usefulyoutube.core.database.entity.VideoRatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoRatingDao {
    @Query("SELECT * FROM video_ratings WHERE video_id = :videoId")
    fun observeRating(videoId: String): Flow<VideoRatingEntity?>

    @Query("SELECT * FROM video_ratings")
    fun observeRatings(): Flow<List<VideoRatingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: VideoRatingEntity)

    @Query("DELETE FROM video_ratings WHERE video_id = :videoId")
    suspend fun delete(videoId: String)
}
