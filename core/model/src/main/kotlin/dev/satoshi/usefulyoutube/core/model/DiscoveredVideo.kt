package dev.satoshi.usefulyoutube.core.model

/**
 * A video of a channel the user is not subscribed to, found for the recommendations. [suggestedBy] is the
 * channel that features the video's channel; `null` when the user watched or liked the channel themselves.
 */
data class DiscoveredVideo(
    val video: Video,
    val suggestedBy: ChannelId?,
)
