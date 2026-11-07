package dev.satoshi.usefulyoutube.core.data.repository

import dev.satoshi.usefulyoutube.core.data.mapper.toSnapshot
import dev.satoshi.usefulyoutube.core.database.dao.MirroredPlaylistDao
import dev.satoshi.usefulyoutube.core.database.entity.MirroredPlaylist
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylist
import dev.satoshi.usefulyoutube.core.domain.repository.MirroredPlaylistRepository
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import javax.inject.Inject
import kotlin.time.Clock

internal class OfflineFirstMirroredPlaylistRepository
    @Inject
    constructor(
        private val mirroredPlaylistDao: MirroredPlaylistDao,
        private val clock: Clock,
    ) : MirroredPlaylistRepository {
        override suspend fun getMirroredContentKinds(): Map<VideoId, VideoContentKind> =
            mirroredPlaylistDao.getStoredVideoKinds().associate { VideoId(it.videoId) to it.contentKind }

        override suspend fun replaceMirroredPlaylists(playlists: List<AccountPlaylist>) =
            mirroredPlaylistDao.replaceAll(
                playlists =
                    playlists.map {
                        MirroredPlaylist(
                            it.remoteId,
                            it.title,
                            it.videos.map { video ->
                                video.toSnapshot()
                            },
                        )
                    },
                syncedAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
    }
