package dev.satoshi.usefulyoutube.core.domain.usecase

import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsCandidate
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val MAXIMUM_CONCURRENT_CLASSIFICATIONS = 8

/** Kinds that are shown to the user: Shorts and unclassified videos stay hidden. */
internal val WATCHABLE_CONTENT_KINDS = setOf(VideoContentKind.REGULAR, VideoContentKind.LIVE_OR_PREMIERE)

/** Classifies the [candidates] in parallel; each classification may check a URL. */
internal suspend fun ClassifyVideoUseCase.classifyAll(
    candidates: List<ShortsCandidate>,
): Map<VideoId, VideoContentKind> =
    coroutineScope {
        val semaphore = Semaphore(MAXIMUM_CONCURRENT_CLASSIFICATIONS)
        candidates
            .map { candidate -> async { semaphore.withPermit { candidate.id to invoke(candidate) } } }
            .awaitAll()
            .toMap()
    }

/** Classifies the [videos] and keeps only the watchable ones with their kinds. */
internal suspend fun ClassifyVideoUseCase.keepWatchable(videos: List<Video>): List<Video> {
    val kinds = classifyAll(videos.map { ShortsCandidate(it.id, it.duration) })
    return videos.mapNotNull { video ->
        kinds[video.id]?.takeIf { it in WATCHABLE_CONTENT_KINDS }?.let { video.copy(contentKind = it) }
    }
}
