package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistUseCase
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) {
        operator fun invoke(id: PlaylistId): Flow<PlaylistWithVideos?> = playlistRepository.observePlaylist(id)
    }
