package dev.satoshi.usefulyoutube.core.model

sealed interface AppError

/** Failures of remote data sources. */
sealed interface DataError : AppError {
    data object NoNetwork : DataError

    data object InvalidApiKey : DataError

    data object QuotaExceeded : DataError

    data object NotFound : DataError

    data class Http(
        val code: Int,
    ) : DataError

    data object Parsing : DataError

    data object Unknown : DataError
}

/** Failures of importing subscriptions from a Google Takeout file. */
sealed interface ImportError : AppError {
    data object UnreadableFile : ImportError

    data object InvalidFormat : ImportError

    data object NoChannels : ImportError
}

/** Failures of adding a channel by a link, a handle or an id. */
sealed interface ChannelError : AppError {
    data object InvalidInput : ChannelError

    data object AlreadySubscribed : ChannelError

    data object NotFound : ChannelError

    data class Remote(
        val cause: DataError,
    ) : ChannelError
}
