package dev.satoshi.usefulyoutube.core.testing.repository

import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError

/** Hands out the [tokens] in order, one per [getAccessToken] call, unless [error] is set. */
class FakeAccountAuthorizer(
    vararg tokens: String = arrayOf("token"),
) : AccountAuthorizer {
    private val tokens = ArrayDeque(tokens.toList())

    var error: AuthError? = null
    val clearedTokens = mutableListOf<String>()

    override suspend fun getAccessToken(): AppResult<String, AuthError> {
        error?.let { return AppResult.Failure(it) }
        val token = if (tokens.size > 1) tokens.removeFirst() else tokens.first()
        return AppResult.Success(token)
    }

    override suspend fun clearAccessToken(token: String) {
        clearedTokens += token
    }
}
