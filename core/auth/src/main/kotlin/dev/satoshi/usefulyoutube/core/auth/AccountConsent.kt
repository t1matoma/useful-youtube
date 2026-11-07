package dev.satoshi.usefulyoutube.core.auth

import android.content.IntentSender
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError

/** The interactive part of the Google authorization, which a screen has to launch itself. */
fun interface AccountConsent {
    /**
     * The Google consent screen to launch with `StartIntentSenderForResult`, or `null` when access to the
     * account is already granted.
     */
    suspend fun consentScreen(): AppResult<IntentSender?, AuthError>
}
