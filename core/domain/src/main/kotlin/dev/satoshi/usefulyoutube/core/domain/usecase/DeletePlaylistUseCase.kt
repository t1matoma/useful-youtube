package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import javax.inject.Inject

class DeletePlaylistUseCase
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) {
        suspend operator fun invoke(id: PlaylistId) = playlistRepository.deletePlaylist(id)
    }
