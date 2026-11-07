package dev.satoshi.usefulyoutube.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.satoshi.usefulyoutube.core.database.UsefulYouTubeDatabase
import dev.satoshi.usefulyoutube.core.database.dao.ChannelDao
import dev.satoshi.usefulyoutube.core.database.dao.MirroredPlaylistDao
import dev.satoshi.usefulyoutube.core.database.dao.PlaylistDao
import dev.satoshi.usefulyoutube.core.database.dao.PlaylistItemDao
import dev.satoshi.usefulyoutube.core.database.dao.RecentSearchQueryDao
import dev.satoshi.usefulyoutube.core.database.dao.SearchDao
import dev.satoshi.usefulyoutube.core.database.dao.VideoDao
import dev.satoshi.usefulyoutube.core.database.dao.WatchHistoryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): UsefulYouTubeDatabase =
        Room
            .databaseBuilder(context, UsefulYouTubeDatabase::class.java, UsefulYouTubeDatabase.NAME)
            .build()

    @Provides
    fun provideChannelDao(database: UsefulYouTubeDatabase): ChannelDao = database.channelDao()

    @Provides
    fun provideVideoDao(database: UsefulYouTubeDatabase): VideoDao = database.videoDao()

    @Provides
    fun provideSearchDao(database: UsefulYouTubeDatabase): SearchDao = database.searchDao()

    @Provides
    fun provideRecentSearchQueryDao(database: UsefulYouTubeDatabase): RecentSearchQueryDao =
        database.recentSearchQueryDao()

    @Provides
    fun providePlaylistDao(database: UsefulYouTubeDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePlaylistItemDao(database: UsefulYouTubeDatabase): PlaylistItemDao = database.playlistItemDao()

    @Provides
    fun provideMirroredPlaylistDao(database: UsefulYouTubeDatabase): MirroredPlaylistDao =
        database.mirroredPlaylistDao()

    @Provides
    fun provideWatchHistoryDao(database: UsefulYouTubeDatabase): WatchHistoryDao = database.watchHistoryDao()
}
