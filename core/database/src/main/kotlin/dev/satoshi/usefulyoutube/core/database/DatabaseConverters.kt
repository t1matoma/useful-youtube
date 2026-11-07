package dev.satoshi.usefulyoutube.core.database

import androidx.room.TypeConverter
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.VideoContentKind

internal class DatabaseConverters {
    @TypeConverter
    fun fromContentKind(kind: VideoContentKind): String = kind.name

    @TypeConverter
    fun toContentKind(name: String): VideoContentKind = VideoContentKind.valueOf(name)

    @TypeConverter
    fun fromSearchOrder(order: SearchOrder): String = order.name

    @TypeConverter
    fun toSearchOrder(name: String): SearchOrder = SearchOrder.valueOf(name)
}
