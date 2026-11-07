package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylistsReader
import dev.satoshi.usefulyoutube.core.domain.repository.MirroredPlaylistRepository
import dev.satoshi.usefulyoutube.core.domain.repository.withAccessToken
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import javax.inject.Inject

/**
 * Updates the read-only copy of the YouTube account's playlists. The account is the source of truth:
 * playlists and videos removed on YouTube disappear from the app. Shorts and unavailable videos are hidden;
 * videos that were classified before are not classified again.
 */
class SyncAccountPlaylistsUseCase
    @Inject
    constructor(
        private val authorizer: AccountAuthorizer,
        private val playlistsReader: AccountPlaylistsReader,
        private val mirroredPlaylists: MirroredPlaylistRepository,
        private val classifyVideo: ClassifyVideoUseCase,
    ) {
        suspend operator fun invoke(): AppResult<Unit, AccountError> {
            val playlists =
                when (val result = authorizer.withAccessToken { playlistsReader.read(it) }) {
                    is AppResult.Failure -> return result
                    is AppResult.Success -> result.value
                }
            val knownKinds = mirroredPlaylists.getMirroredContentKinds()
            val newCandidates =
                playlists
                    .flatMap { it.videos }
                    .distinctBy { it.id }
                    .filterNot { it.id in knownKinds }
                    .map { ShortsCandidate(it.id, it.duration) }
            val kinds = knownKinds + classifyVideo.classifyAll(newCandidates)
            mirroredPlaylists.replaceMirroredPlaylists(
                playlists.map { playlist ->
                    playlist.copy(
                        videos =
                            playlist.videos.mapNotNull { video ->
                                kinds[video.id]
                                    ?.takeIf { it in WATCHABLE_CONTENT_KINDS }
                                    ?.let { video.copy(contentKind = it) }
                            },
                    )
                },
            )
            return AppResult.Success(Unit)
        }
    }
