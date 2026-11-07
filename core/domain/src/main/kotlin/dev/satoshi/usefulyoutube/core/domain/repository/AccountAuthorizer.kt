package dev.satoshi.usefulyoutube.core.domain.repository

import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError

/** Grants OAuth access to the user's YouTube account. */
interface AccountAuthorizer {
    /**
     * Returns an access token without user interaction, or [AuthError.ConsentRequired] when the user has to
     * grant access on the consent screen first.
     */
    suspend fun getAccessToken(): AppResult<String, AuthError>

    /** Forgets a token rejected by the API, so the next [getAccessToken] returns a fresh one. */
    suspend fun clearAccessToken(token: String)
}
