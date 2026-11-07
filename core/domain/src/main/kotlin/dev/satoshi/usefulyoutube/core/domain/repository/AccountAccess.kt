package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError

/**
 * Runs [request] with an access token of the account. A cached token can expire or be revoked, so a token
 * rejected with [DataError.Unauthorized] is cleared and replaced once.
 */
suspend fun <T> AccountAuthorizer.withAccessToken(
    request: suspend (accessToken: String) -> AppResult<T, DataError>,
): AppResult<T, AccountError> = withAccessToken(request, retryUnauthorized = true)

private suspend fun <T> AccountAuthorizer.withAccessToken(
    request: suspend (accessToken: String) -> AppResult<T, DataError>,
    retryUnauthorized: Boolean,
): AppResult<T, AccountError> {
    val token =
        when (val result = getAccessToken()) {
            is AppResult.Failure -> return AppResult.Failure(AccountError.Auth(result.error))
            is AppResult.Success -> result.value
        }
    return when (val result = request(token)) {
        is AppResult.Success -> {
            result
        }

        is AppResult.Failure -> {
            if (result.error == DataError.Unauthorized && retryUnauthorized) {
                clearAccessToken(token)
                withAccessToken(request, retryUnauthorized = false)
            } else {
                AppResult.Failure(AccountError.Remote(result.error))
            }
        }
    }
}
