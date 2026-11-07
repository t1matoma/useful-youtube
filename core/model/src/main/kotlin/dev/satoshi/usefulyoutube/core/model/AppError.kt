package dev.satoshi.usefulyoutube.core.model

sealed interface AppError

/** Failures of remote data sources. */
sealed interface DataError : AppError {
    data object NoNetwork : DataError

    data object InvalidApiKey : DataError

    /** The OAuth access token is missing, expired or revoked. */
    data object Unauthorized : DataError

    data object QuotaExceeded : DataError

    data object NotFound : DataError

    data class Http(
        val code: Int,
    ) : DataError

    data object Parsing : DataError

    data object Unknown : DataError
}

/** Failures of authorizing access to the user's Google account. */
sealed interface AuthError : AppError {
    /** The user has to grant access on the Google consent screen first. */
    data object ConsentRequired : AuthError

    data object Cancelled : AuthError

    /** Google Play services are missing, disabled or outdated. */
    data object Unavailable : AuthError

    data object Failed : AuthError
}

/** Failures of importing subscriptions from a Google Takeout file or from the YouTube account. */
sealed interface ImportError : AppError {
    data object UnreadableFile : ImportError

    data object InvalidFormat : ImportError

    data object NoChannels : ImportError

    data class Auth(
        val cause: AuthError,
    ) : ImportError

    data class Remote(
        val cause: DataError,
    ) : ImportError
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
