package dev.satoshi.usefulyoutube.core.domain.recommendations

import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.model.WatchedVideo

private const val WATCH_WEIGHT = 1.0
private const val LIKE_WEIGHT = 3.0
private const val DISLIKE_WEIGHT = -3.0

/**
 * How much the user likes each channel: every watched video adds [WATCH_WEIGHT], a like [LIKE_WEIGHT] and a
 * dislike [DISLIKE_WEIGHT]. Channels the user never watched or rated are absent.
 */
internal fun channelAffinities(
    history: List<WatchedVideo>,
    ratedVideos: List<RatedVideo>,
): Map<ChannelId, Double> {
    val affinities = mutableMapOf<ChannelId, Double>()
    history.forEach { affinities.merge(it.video.channelId, WATCH_WEIGHT, Double::plus) }
    ratedVideos.forEach { rated ->
        val weight =
            when (rated.rating) {
                VideoRating.LIKE -> LIKE_WEIGHT
                VideoRating.DISLIKE -> DISLIKE_WEIGHT
                VideoRating.NONE -> return@forEach
            }
        affinities.merge(rated.channelId, weight, Double::plus)
    }
    return affinities
}
