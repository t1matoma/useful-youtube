package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.model.Chapter
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** A stored video with the chapters listed in its description; [chapters] is empty when there are none. */
data class VideoWithChapters(
    val video: Video,
    val chapters: List<Chapter>,
)

/** The video for the player, see [ObserveVideoUseCase], together with its chapters. */
class ObserveVideoWithChaptersUseCase
    @Inject
    constructor(
        private val observeVideo: ObserveVideoUseCase,
        private val parseChapters: ParseChaptersUseCase,
    ) {
        operator fun invoke(id: VideoId): Flow<VideoWithChapters?> =
            observeVideo(id).map { video ->
                video?.let { VideoWithChapters(it, parseChapters(it.description, it.duration)) }
            }
    }
