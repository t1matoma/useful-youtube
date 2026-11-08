package dev.satoshi.usefulyoutube.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.satoshi.usefulyoutube.core.database.dao.ChannelDao
import dev.satoshi.usefulyoutube.core.database.dao.MirroredPlaylistDao
import dev.satoshi.usefulyoutube.core.database.dao.PlaylistDao
import dev.satoshi.usefulyoutube.core.database.dao.PlaylistItemDao
import dev.satoshi.usefulyoutube.core.database.dao.RecentSearchQueryDao
import dev.satoshi.usefulyoutube.core.database.dao.SearchDao
import dev.satoshi.usefulyoutube.core.database.dao.VideoDao
import dev.satoshi.usefulyoutube.core.database.dao.WatchHistoryDao
import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistEntity
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistItemEntity
import dev.satoshi.usefulyoutube.core.database.entity.RecentSearchQueryEntity
import dev.satoshi.usefulyoutube.core.database.entity.SearchResultEntity
import dev.satoshi.usefulyoutube.core.database.entity.SearchSessionEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoEntity
import dev.satoshi.usefulyoutube.core.database.entity.WatchHistoryEntity

@Database(
    entities = [
        ChannelEntity::class,
        VideoEntity::class,
        SearchResultEntity::class,
        SearchSessionEntity::class,
        RecentSearchQueryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        WatchHistoryEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        // Version 5 adds the playback position to the watch history.
        AutoMigration(from = 4, to = 5),
        // Version 2 adds the search tables.
        AutoMigration(from = 1, to = 2),
        // Version 3 adds the playlist tables.
        AutoMigration(from = 2, to = 3),
        // Version 4 adds the watch history.
        AutoMigration(from = 3, to = 4),
    ],
)
@TypeConverters(DatabaseConverters::class)
abstract class UsefulYouTubeDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    abstract fun videoDao(): VideoDao

    abstract fun searchDao(): SearchDao

    abstract fun recentSearchQueryDao(): RecentSearchQueryDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun playlistItemDao(): PlaylistItemDao

    abstract fun mirroredPlaylistDao(): MirroredPlaylistDao

    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        const val NAME = "useful_youtube.db"
    }
}
