package dev.satoshi.usefulyoutube.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.satoshi.usefulyoutube.core.database.dao.ChannelDao
import dev.satoshi.usefulyoutube.core.database.dao.RecentSearchQueryDao
import dev.satoshi.usefulyoutube.core.database.dao.SearchDao
import dev.satoshi.usefulyoutube.core.database.dao.VideoDao
import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import dev.satoshi.usefulyoutube.core.database.entity.RecentSearchQueryEntity
import dev.satoshi.usefulyoutube.core.database.entity.SearchResultEntity
import dev.satoshi.usefulyoutube.core.database.entity.SearchSessionEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoEntity

@Database(
    entities = [
        ChannelEntity::class,
        VideoEntity::class,
        SearchResultEntity::class,
        SearchSessionEntity::class,
        RecentSearchQueryEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // Version 2 adds the search tables.
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(DatabaseConverters::class)
abstract class UsefulYouTubeDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    abstract fun videoDao(): VideoDao

    abstract fun searchDao(): SearchDao

    abstract fun recentSearchQueryDao(): RecentSearchQueryDao

    companion object {
        const val NAME = "useful_youtube.db"
    }
}
