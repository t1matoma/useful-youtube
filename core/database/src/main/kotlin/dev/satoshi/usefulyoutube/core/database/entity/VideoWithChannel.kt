package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class VideoWithChannel(
    @Embedded val video: VideoEntity,
    @ColumnInfo(name = "channel_title") val channelTitle: String,
)
