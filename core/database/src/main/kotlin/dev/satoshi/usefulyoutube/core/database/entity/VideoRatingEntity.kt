package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.satoshi.usefulyoutube.core.model.VideoRating

/** A like or a dislike; videos without a rating have no row. The channel is kept for the recommendations. */
@Entity(tableName = "video_ratings")
data class VideoRatingEntity(
    @PrimaryKey @ColumnInfo(name = "video_id") val videoId: String,
    @ColumnInfo(name = "channel_id") val channelId: String,
    val rating: VideoRating,
)
