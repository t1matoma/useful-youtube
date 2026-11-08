package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index

/**
 * A watched video; watching it again only moves it to the top. The playback position is where the video
 * stopped, `0` until it is saved; the playback duration is the video's length reported by the player.
 */
@Entity(tableName = "watch_history", primaryKeys = ["video_id"], indices = [Index("watched_at")])
data class WatchHistoryEntity(
    @ColumnInfo(name = "watched_at") val watchedAtEpochMillis: Long,
    @Embedded val video: VideoSnapshot,
    @ColumnInfo(name = "playback_position_seconds", defaultValue = "0") val playbackPositionSeconds: Long = 0,
    @ColumnInfo(name = "playback_duration_seconds", defaultValue = "0") val playbackDurationSeconds: Long = 0,
)
