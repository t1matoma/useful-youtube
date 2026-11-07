package dev.satoshi.usefulyoutube.core.data.mapper

import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.network.model.SubscriptionItemDto

internal fun ChannelEntity.toChannel() = Channel(id = ChannelId(id), title = title)

internal fun Channel.toEntity() = ChannelEntity(id = id.value, title = title)

internal fun SubscriptionItemDto.toChannel() =
    Channel(id = ChannelId(snippet.resourceId.channelId), title = snippet.title)
