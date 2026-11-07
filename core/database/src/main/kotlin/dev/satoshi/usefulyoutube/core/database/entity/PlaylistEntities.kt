package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** [remoteId] is the YouTube playlist id of a mirrored account playlist and `null` for app playlists. */
@Entity(tableName = "playlists", indices = [Index(value = ["remote_id"], unique = true)])
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    /** When the mirrored playlist was last loaded from YouTube. */
    @ColumnInfo(name = "synced_at") val syncedAtEpochMillis: Long? = null,
)

/** A video in a playlist; a playlist contains each video at most once. */
@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlist_id", "video_id"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("video_id")],
)
data class PlaylistItemEntity(
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    val position: Int,
    @Embedded val video: VideoSnapshot,
)

data class PlaylistSummary(
    @Embedded val playlist: PlaylistEntity,
    @ColumnInfo(name = "video_count") val videoCount: Int,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
)
