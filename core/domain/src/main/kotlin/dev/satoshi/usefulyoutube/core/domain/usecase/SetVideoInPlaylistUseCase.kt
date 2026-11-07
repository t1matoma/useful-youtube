package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.Video
import javax.inject.Inject

/** Adds the [video] to the playlist or removes it, as the user ticks the playlist. */
class SetVideoInPlaylistUseCase
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) {
        suspend operator fun invoke(
            id: PlaylistId,
            video: Video,
            inPlaylist: Boolean,
        ) = if (inPlaylist) {
            playlistRepository.addVideo(id, video)
        } else {
            playlistRepository.removeVideo(id, video.id)
        }
    }
