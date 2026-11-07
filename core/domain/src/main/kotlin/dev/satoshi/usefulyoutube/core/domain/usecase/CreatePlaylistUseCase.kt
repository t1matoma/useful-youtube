package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.Video
import javax.inject.Inject

/** Creates an app playlist, optionally with its [firstVideo]; returns `null` for a blank title. */
class CreatePlaylistUseCase
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) {
        suspend operator fun invoke(
            title: String,
            firstVideo: Video? = null,
        ): PlaylistId? {
            val normalizedTitle = title.trim().takeIf { it.isNotEmpty() } ?: return null
            val id = playlistRepository.createPlaylist(normalizedTitle)
            firstVideo?.let { playlistRepository.addVideo(id, it) }
            return id
        }
    }
