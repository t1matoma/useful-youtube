package dev.satoshi.usefulyoutube.core.database.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.satoshi.usefulyoutube.core.database.UsefulYouTubeDatabase
import dev.satoshi.usefulyoutube.core.database.dao.DiscoveryDao
import dev.satoshi.usefulyoutube.core.database.dao.VideoRatingDao

/** DAOs of the likes and the recommendations. */
@Module
@InstallIn(SingletonComponent::class)
internal object PersonalizationDaoModule {
    @Provides
    fun provideVideoRatingDao(database: UsefulYouTubeDatabase): VideoRatingDao = database.videoRatingDao()

    @Provides
    fun provideDiscoveryDao(database: UsefulYouTubeDatabase): DiscoveryDao = database.discoveryDao()
}
