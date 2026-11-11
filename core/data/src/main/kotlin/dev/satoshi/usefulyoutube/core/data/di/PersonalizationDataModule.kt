package dev.satoshi.usefulyoutube.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstRecommendationRepository
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstVideoRatingRepository
import dev.satoshi.usefulyoutube.core.data.repository.PreferencesSettingsRepository
import dev.satoshi.usefulyoutube.core.domain.repository.RecommendationRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SettingsRepository
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRatingRepository
import javax.inject.Singleton

/** Likes, settings and recommendations. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PersonalizationDataModule {
    @Binds
    @Singleton
    abstract fun bindVideoRatingRepository(repository: OfflineFirstVideoRatingRepository): VideoRatingRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: PreferencesSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(
        repository: OfflineFirstRecommendationRepository,
    ): RecommendationRepository
}
