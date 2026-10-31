package dev.satoshi.usefulyoutube.core.data.mapper

import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId

internal fun ChannelEntity.toChannel() = Channel(id = ChannelId(id), title = title)

internal fun Channel.toEntity() = ChannelEntity(id = id.value, title = title)
