package dev.satoshi.usefulyoutube.core.database

import androidx.room.TypeConverter
import dev.satoshi.usefulyoutube.core.model.VideoContentKind

internal class DatabaseConverters {
    @TypeConverter
    fun fromContentKind(kind: VideoContentKind): String = kind.name

    @TypeConverter
    fun toContentKind(name: String): VideoContentKind = VideoContentKind.valueOf(name)
}
