package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import javax.inject.Inject

/** Blank titles are ignored. */
class RenamePlaylistUseCase
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) {
        suspend operator fun invoke(
            id: PlaylistId,
            title: String,
        ) {
            val normalizedTitle = title.trim().takeIf { it.isNotEmpty() } ?: return
            playlistRepository.renamePlaylist(id, normalizedTitle)
        }
    }
