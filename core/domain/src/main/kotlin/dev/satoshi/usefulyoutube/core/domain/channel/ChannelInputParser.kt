package dev.satoshi.usefulyoutube.core.domain.channel

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import javax.inject.Inject

/**
 * Parses user input into a [ChannelReference]. Accepts a channel id (`UC…`), a handle (`@name`)
 * and YouTube links containing either of them (`youtube.com/channel/UC…`, `youtube.com/@name`).
 */
class ChannelInputParser
    @Inject
    constructor() {
        fun parse(input: String): AppResult<ChannelReference, ChannelError> {
            val trimmed = input.trim()
            val channelId = firstGroup(trimmed, CHANNEL_ID_REGEX, CHANNEL_URL_REGEX)
            val handle = firstGroup(trimmed, HANDLE_REGEX, HANDLE_URL_REGEX)
            val reference =
                when {
                    channelId != null -> ChannelReference.ById(ChannelId(channelId))
                    handle != null -> ChannelReference.ByHandle(handle)
                    else -> return AppResult.Failure(ChannelError.InvalidInput)
                }
            return AppResult.Success(reference)
        }

        private fun firstGroup(
            input: String,
            vararg patterns: Regex,
        ): String? = patterns.firstNotNullOfOrNull { it.matchEntire(input) }?.groupValues?.get(1)

        private companion object {
            const val CHANNEL_ID = "(UC[A-Za-z0-9_-]{22})"
            const val HANDLE = "([A-Za-z0-9_.-]{3,30})"
            const val HOST = "(?i:(?:https?://)?(?:www\\.|m\\.)?youtube\\.com)"
            const val SUFFIX = "(?:[/?#].*)?"

            val CHANNEL_ID_REGEX = Regex(CHANNEL_ID)
            val HANDLE_REGEX = Regex("@$HANDLE")
            val CHANNEL_URL_REGEX = Regex("$HOST/channel/$CHANNEL_ID$SUFFIX")
            val HANDLE_URL_REGEX = Regex("$HOST/@$HANDLE$SUFFIX")
        }
    }
