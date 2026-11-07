package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index

/** A watched video; watching it again only moves it to the top. */
@Entity(tableName = "watch_history", primaryKeys = ["video_id"], indices = [Index("watched_at")])
data class WatchHistoryEntity(
    @ColumnInfo(name = "watched_at") val watchedAtEpochMillis: Long,
    @Embedded val video: VideoSnapshot,
)
