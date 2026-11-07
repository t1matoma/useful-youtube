package dev.satoshi.usefulyoutube.core.data.playlists

import dev.satoshi.usefulyoutube.core.data.di.IoDispatcher
import dev.satoshi.usefulyoutube.core.data.mapper.toFoundVideo
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylist
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylistsReader
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.network.YouTubeNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Loads the playlists (1 quota unit per 50), their video ids (1 unit per 50 per playlist) and the details
 * of all their videos at once (1 unit per 50). Private and deleted videos have no details and are skipped.
 */
internal class NetworkAccountPlaylistsReader
    @Inject
    constructor(
        private val networkDataSource: YouTubeNetworkDataSource,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AccountPlaylistsReader {
        override suspend fun read(accessToken: String): AppResult<List<AccountPlaylist>, DataError> =
            withContext(ioDispatcher) {
                val playlists =
                    when (val result = networkDataSource.getMyPlaylists(accessToken)) {
                        is AppResult.Failure -> return@withContext result
                        is AppResult.Success -> result.value
                    }
                val videoIds =
                    playlists.associate { playlist ->
                        when (val result = networkDataSource.getPlaylistVideoIds(accessToken, playlist.id)) {
                            is AppResult.Failure -> return@withContext result
                            is AppResult.Success -> playlist.id to result.value
                        }
                    }
                val allIds = videoIds.values.flatten().distinct()
                val details =
                    if (allIds.isEmpty()) {
                        emptyMap()
                    } else {
                        when (val result = networkDataSource.getVideoDetails(allIds)) {
                            is AppResult.Failure -> {
                                return@withContext result
                            }

                            is AppResult.Success -> {
                                result.value
                                    .mapNotNull {
                                        it.toFoundVideo()
                                    }.associateBy { it.id.value }
                            }
                        }
                    }
                AppResult.Success(
                    playlists.map { playlist ->
                        AccountPlaylist(
                            remoteId = playlist.id,
                            title = playlist.snippet.title,
                            videos = videoIds.getValue(playlist.id).mapNotNull { details[it] },
                        )
                    },
                )
            }
    }
