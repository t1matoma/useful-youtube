package dev.satoshi.usefulyoutube.core.auth

import android.content.Context
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.satoshi.usefulyoutube.core.domain.repository.AccountAuthorizer
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authorization with the Google Identity `AuthorizationClient`. It needs no backend and no client id in
 * the code: Google matches the app by its package name and signing certificate. Access tokens are cached
 * and renewed by Google Play services, so a repeated [getAccessToken] does not show any UI.
 */
@Singleton
internal class GoogleAccountAuthorization
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : AccountAuthorizer,
        AccountConsent {
        private val client by lazy { Identity.getAuthorizationClient(context) }
        private val request =
            AuthorizationRequest
                .builder()
                .setRequestedScopes(listOf(Scope(YOUTUBE_SCOPE)))
                .build()

        override suspend fun getAccessToken(): AppResult<String, AuthError> =
            when (val result = authorize()) {
                is AppResult.Failure -> {
                    result
                }

                is AppResult.Success -> {
                    val token = result.value.accessToken
                    when {
                        result.value.hasResolution() -> AppResult.Failure(AuthError.ConsentRequired)
                        token.isNullOrEmpty() -> AppResult.Failure(AuthError.Failed)
                        else -> AppResult.Success(token)
                    }
                }
            }

        override suspend fun clearAccessToken(token: String) {
            try {
                client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
            } catch (_: ApiException) {
                // The next authorization asks Google Play services again, so a failed cleanup is harmless.
            }
        }

        override suspend fun consentScreen(): AppResult<IntentSender?, AuthError> =
            authorize().map { result ->
                result.pendingIntent?.intentSender?.takeIf { result.hasResolution() }
            }

        private suspend fun authorize(): AppResult<AuthorizationResult, AuthError> =
            try {
                AppResult.Success(client.authorize(request).await())
            } catch (exception: ApiException) {
                AppResult.Failure(authErrorOf(exception.statusCode))
            }

        private companion object {
            /** Full access: reading subscriptions now, managing playlists later. */
            const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"
        }
    }

/** Maps a Google Play services status code of a failed authorization to [AuthError]. */
internal fun authErrorOf(statusCode: Int): AuthError =
    when (statusCode) {
        CommonStatusCodes.CANCELED -> {
            AuthError.Cancelled
        }

        ConnectionResult.SERVICE_MISSING,
        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
        ConnectionResult.SERVICE_DISABLED,
        ConnectionResult.SERVICE_INVALID,
        CommonStatusCodes.API_NOT_CONNECTED,
        -> {
            AuthError.Unavailable
        }

        else -> {
            AuthError.Failed
        }
    }
