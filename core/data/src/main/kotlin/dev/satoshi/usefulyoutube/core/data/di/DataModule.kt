package dev.satoshi.usefulyoutube.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstChannelRepository
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstVideoRepository
import dev.satoshi.usefulyoutube.core.data.shorts.NetworkShortsUrlProbe
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.domain.shorts.CompositeShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsUrlProbe
import dev.satoshi.usefulyoutube.core.domain.shorts.UrlShortsDetector
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindChannelRepository(repository: OfflineFirstChannelRepository): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(repository: OfflineFirstVideoRepository): VideoRepository

    @Binds
    abstract fun bindShortsUrlProbe(probe: NetworkShortsUrlProbe): ShortsUrlProbe

    companion object {
        /** Duration screens out long videos; the `/shorts/` URL check confirms short ones. */
        @Provides
        fun provideShortsDetector(probe: ShortsUrlProbe): ShortsDetector =
            CompositeShortsDetector(screening = DurationShortsDetector(), confirmation = UrlShortsDetector(probe))
    }
}
