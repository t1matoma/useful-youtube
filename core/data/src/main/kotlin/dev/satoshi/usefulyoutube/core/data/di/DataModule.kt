package dev.satoshi.usefulyoutube.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.satoshi.usefulyoutube.core.data.playlists.NetworkAccountPlaylistsReader
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstChannelRepository
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstMirroredPlaylistRepository
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstPlaylistRepository
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstSearchRepository
import dev.satoshi.usefulyoutube.core.data.repository.OfflineFirstVideoRepository
import dev.satoshi.usefulyoutube.core.data.shorts.NetworkShortsUrlProbe
import dev.satoshi.usefulyoutube.core.data.subscriptions.ContentResolverSubscriptionsFileReader
import dev.satoshi.usefulyoutube.core.data.subscriptions.NetworkAccountSubscriptionsReader
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylistsReader
import dev.satoshi.usefulyoutube.core.domain.repository.AccountSubscriptionsReader
import dev.satoshi.usefulyoutube.core.domain.repository.ChannelRepository
import dev.satoshi.usefulyoutube.core.domain.repository.MirroredPlaylistRepository
import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SearchRepository
import dev.satoshi.usefulyoutube.core.domain.repository.SubscriptionsFileReader
import dev.satoshi.usefulyoutube.core.domain.repository.VideoRepository
import dev.satoshi.usefulyoutube.core.domain.shorts.CompositeShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.DurationShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsUrlProbe
import dev.satoshi.usefulyoutube.core.domain.shorts.UrlShortsDetector
import java.util.Locale
import javax.inject.Singleton
import kotlin.time.Clock

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
    @Singleton
    abstract fun bindSearchRepository(repository: OfflineFirstSearchRepository): SearchRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(repository: OfflineFirstPlaylistRepository): PlaylistRepository

    @Binds
    abstract fun bindMirroredPlaylistRepository(
        repository: OfflineFirstMirroredPlaylistRepository,
    ): MirroredPlaylistRepository

    @Binds
    abstract fun bindAccountPlaylistsReader(reader: NetworkAccountPlaylistsReader): AccountPlaylistsReader

    @Binds
    abstract fun bindSubscriptionsFileReader(reader: ContentResolverSubscriptionsFileReader): SubscriptionsFileReader

    @Binds
    abstract fun bindAccountSubscriptionsReader(reader: NetworkAccountSubscriptionsReader): AccountSubscriptionsReader

    @Binds
    abstract fun bindShortsUrlProbe(probe: NetworkShortsUrlProbe): ShortsUrlProbe

    companion object {
        /** Read on every search, so a changed device language applies without a restart. */
        @Provides
        fun provideLocale(): Locale = Locale.getDefault()

        @Provides
        fun provideClock(): Clock = Clock.System

        /** Duration screens out long videos; the `/shorts/` URL check confirms short ones. */
        @Provides
        fun provideShortsDetector(probe: ShortsUrlProbe): ShortsDetector =
            CompositeShortsDetector(screening = DurationShortsDetector(), confirmation = UrlShortsDetector(probe))
    }
}
