package dev.satoshi.usefulyoutube.core.domain.recommendations

import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Everything the recommendations are computed from; all of it is stored on the device. */
data class RecommendationSignals(
    /** Unwatched and watched videos of the subscriptions. */
    val feed: List<Video>,
    val discovered: List<DiscoveredVideo>,
    val history: List<WatchedVideo>,
    val ratedVideos: List<RatedVideo>,
)

/**
 * Orders videos the user has not watched or rated by how much they like the channel and how new the video is:
 * `(1 + channel affinity) × freshness`. A channel the user does not know inherits [SUGGESTED_CHANNEL_FACTOR] of
 * the affinity of the channel that features it. Channels with more dislikes than interest are left out, and one
 * channel gets at most [MAXIMUM_VIDEOS_PER_CHANNEL] places, so a frequent uploader does not fill the list.
 */
class RecommendationRanker
    @Inject
    constructor() {
        fun rank(
            signals: RecommendationSignals,
            now: Instant,
        ): List<Video> {
            val affinities = channelAffinities(signals.history, signals.ratedVideos)
            val seenIds = signals.history.map { it.video.id }.toSet() + signals.ratedVideos.map { it.videoId }
            val feedCandidates = signals.feed.map { it to (affinities[it.channelId] ?: 0.0) }
            val discoveredCandidates = signals.discovered.map { it.video to it.channelScore(affinities) }
            val placesPerChannel = mutableMapOf<ChannelId, Int>()
            return (feedCandidates + discoveredCandidates)
                .asSequence()
                .filter { (video, channelScore) -> video.id !in seenIds && channelScore >= 0 }
                .distinctBy { (video, _) -> video.id }
                .sortedByDescending { (video, channelScore) -> (1 + channelScore) * freshness(video, now) }
                .map { (video, _) -> video }
                .filter { video ->
                    val place = placesPerChannel.getOrDefault(video.channelId, 0) + 1
                    placesPerChannel[video.channelId] = place
                    place <= MAXIMUM_VIDEOS_PER_CHANNEL
                }.take(MAXIMUM_RECOMMENDATIONS)
                .toList()
        }

        private fun DiscoveredVideo.channelScore(affinities: Map<ChannelId, Double>): Double =
            affinities[video.channelId]
                ?: suggestedBy?.let { affinities[it] }?.times(SUGGESTED_CHANNEL_FACTOR)
                ?: 0.0

        /** `1` for a video published now, a half after [FRESHNESS_HALF_LIFE], and so on. */
        private fun freshness(
            video: Video,
            now: Instant,
        ): Double {
            val age = (now - video.publishedAt).coerceAtLeast(Duration.ZERO)
            return 1 / (1 + age / FRESHNESS_HALF_LIFE)
        }

        private companion object {
            const val SUGGESTED_CHANNEL_FACTOR = 0.5
            const val MAXIMUM_VIDEOS_PER_CHANNEL = 3
            const val MAXIMUM_RECOMMENDATIONS = 100
            val FRESHNESS_HALF_LIFE = 7.days
        }
    }
