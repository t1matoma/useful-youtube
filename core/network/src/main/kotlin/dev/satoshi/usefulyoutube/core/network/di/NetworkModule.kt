package dev.satoshi.usefulyoutube.core.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.satoshi.usefulyoutube.core.network.BuildConfig
import dev.satoshi.usefulyoutube.core.network.ChannelSectionsNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.RetrofitChannelSectionsNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.RetrofitVideoRatingNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.RetrofitYouTubeNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.VideoRatingNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.YouTubeEndpoints
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import dev.satoshi.usefulyoutube.core.network.api.ApiKeyInterceptor
import dev.satoshi.usefulyoutube.core.network.api.YouTubeDataApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/** HTTP client that does not follow redirects, used to check `/shorts/{id}` URLs. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ShortsProbeClient

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {
    @Binds
    abstract fun bindNetworkDataSource(dataSource: RetrofitYouTubeNetworkDataSource): YouTubeNetworkDataSource

    @Binds
    abstract fun bindVideoRatingDataSource(
        dataSource: RetrofitVideoRatingNetworkDataSource,
    ): VideoRatingNetworkDataSource

    @Binds
    abstract fun bindChannelSectionsDataSource(
        dataSource: RetrofitChannelSectionsNetworkDataSource,
    ): ChannelSectionsNetworkDataSource

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }

        @Provides
        @Singleton
        fun provideEndpoints(): YouTubeEndpoints = YouTubeEndpoints()

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

        @Provides
        @Singleton
        @ShortsProbeClient
        fun provideShortsProbeClient(client: OkHttpClient): OkHttpClient =
            client
                .newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

        @Provides
        @Singleton
        fun provideYouTubeDataApi(
            client: OkHttpClient,
            json: Json,
            endpoints: YouTubeEndpoints,
        ): YouTubeDataApi =
            Retrofit
                .Builder()
                .baseUrl(endpoints.dataApiBaseUrl)
                .client(client.newBuilder().addInterceptor(ApiKeyInterceptor(BuildConfig.YOUTUBE_API_KEY)).build())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(YouTubeDataApi::class.java)
    }
}
