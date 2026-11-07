package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.Playlist
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistsUseCase
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) {
        operator fun invoke(): Flow<List<Playlist>> = playlistRepository.observePlaylists()
    }
