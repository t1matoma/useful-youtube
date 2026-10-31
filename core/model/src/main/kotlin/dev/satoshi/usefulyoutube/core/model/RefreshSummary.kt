package dev.satoshi.usefulyoutube.core.model

data class RefreshSummary(
    val newVideoCount: Int,
    val failedChannels: List<ChannelId>,
)
