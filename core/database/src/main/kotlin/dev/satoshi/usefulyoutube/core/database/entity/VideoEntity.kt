package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.satoshi.usefulyoutube.core.model.VideoContentKind

@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channel_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("channel_id"), Index("published_at")],
)
data class VideoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "channel_id") val channelId: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "published_at") val publishedAtEpochMillis: Long,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    @ColumnInfo(name = "content_kind") val contentKind: VideoContentKind,
    @ColumnInfo(name = "details_fetched") val detailsFetched: Boolean,
)

/** Partial update applied after loading details from the Data API. */
data class VideoDetailsUpdate(
    val id: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    @ColumnInfo(name = "details_fetched") val detailsFetched: Boolean = true,
)

data class VideoContentKindUpdate(
    val id: String,
    @ColumnInfo(name = "content_kind") val contentKind: VideoContentKind,
)

data class UnclassifiedVideo(
    val id: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long?,
)
