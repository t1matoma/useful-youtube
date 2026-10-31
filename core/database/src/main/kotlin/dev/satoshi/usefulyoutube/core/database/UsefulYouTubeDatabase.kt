package dev.satoshi.usefulyoutube.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.satoshi.usefulyoutube.core.database.dao.ChannelDao
import dev.satoshi.usefulyoutube.core.database.dao.VideoDao
import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoEntity

@Database(
    entities = [ChannelEntity::class, VideoEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class UsefulYouTubeDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    abstract fun videoDao(): VideoDao

    companion object {
        const val NAME = "useful_youtube.db"
    }
}
