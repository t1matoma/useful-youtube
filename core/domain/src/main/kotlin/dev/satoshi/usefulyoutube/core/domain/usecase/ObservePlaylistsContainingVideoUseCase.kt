package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.repository.PlaylistRepository
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistsContainingVideoUseCase
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) {
        operator fun invoke(videoId: VideoId): Flow<Set<PlaylistId>> =
            playlistRepository.observePlaylistsContaining(videoId)
    }
