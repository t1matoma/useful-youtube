package dev.satoshi.usefulyoutube.feature.player.rating

import android.content.IntentSender
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.VideoRating

data class RatingUiState(
    /** The rating to show: the one being sent, otherwise the stored one. */
    val rating: VideoRating = VideoRating.NONE,
    /** A rating is on its way to YouTube; the buttons wait for the answer. */
    val isUpdating: Boolean = false,
    /** Why the last rating was not accepted; cleared by the next attempt. */
    val error: AccountError? = null,
    /** The screen launches it and reports back with [RatingUiEvent.ConsentFinished]. */
    val consentScreen: IntentSender? = null,
)
