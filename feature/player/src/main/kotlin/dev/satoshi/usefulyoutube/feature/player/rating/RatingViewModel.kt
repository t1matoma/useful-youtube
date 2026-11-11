package dev.satoshi.usefulyoutube.feature.player.rating

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.satoshi.usefulyoutube.core.auth.AccountConsent
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoRatingUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RateVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshVideoRatingUseCase
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The like and dislike buttons of the player, kept apart from the playback state. The rating lives in the
 * YouTube account: it is read when a video opens and changed only after YouTube accepted it. Without a
 * connected account the first rating shows the Google consent screen and is sent once access is granted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RatingViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observeVideoRating: ObserveVideoRatingUseCase,
        private val observeVideo: ObserveVideoUseCase,
        private val refreshVideoRating: RefreshVideoRatingUseCase,
        private val rateVideo: RateVideoUseCase,
        private val accountConsent: AccountConsent,
    ) : ViewModel() {
        private data class RequestState(
            /** The rating that is being sent or waits for the consent. */
            val pendingRating: VideoRating? = null,
            val isUpdating: Boolean = false,
            val error: AccountError? = null,
            val consentScreen: IntentSender? = null,
            val isAwaitingConsent: Boolean = false,
        )

        private val videoId =
            MutableStateFlow(VideoId(checkNotNull(savedStateHandle[PlayerDestination.VIDEO_ID_ARGUMENT])))
        private val requestState = MutableStateFlow(RequestState())

        val uiState: StateFlow<RatingUiState> =
            combine(videoId.flatMapLatest { observeVideoRating(it) }, requestState) { stored, request ->
                RatingUiState(
                    rating = request.pendingRating ?: stored,
                    isUpdating = request.isUpdating,
                    error = request.error,
                    consentScreen = request.consentScreen,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RatingUiState())

        init {
            refresh()
        }

        fun onEvent(event: RatingUiEvent) {
            when (event) {
                is RatingUiEvent.VideoChanged -> {
                    if (event.videoId != videoId.value) {
                        videoId.value = event.videoId
                        requestState.value = RequestState()
                        refresh()
                    }
                }

                RatingUiEvent.LikeClicked -> {
                    toggle(VideoRating.LIKE)
                }

                RatingUiEvent.DislikeClicked -> {
                    toggle(VideoRating.DISLIKE)
                }

                RatingUiEvent.ConsentLaunched -> {
                    requestState.update { it.copy(consentScreen = null, isAwaitingConsent = it.consentScreen != null) }
                }

                is RatingUiEvent.ConsentFinished -> {
                    onConsentFinished(event.granted)
                }
            }
        }

        /** A like given in another app shows up; a failure, such as no connected account, changes nothing. */
        private fun refresh() {
            val id = videoId.value
            viewModelScope.launch { observeVideo(id).first()?.let { refreshVideoRating(it) } }
        }

        private fun toggle(rating: VideoRating) {
            val request = requestState.value
            if (request.isUpdating || request.isAwaitingConsent || request.consentScreen != null) return
            send(if (uiState.value.rating == rating) VideoRating.NONE else rating, mayAskForConsent = true)
        }

        private fun send(
            rating: VideoRating,
            mayAskForConsent: Boolean,
        ) {
            val id = videoId.value
            requestState.value = RequestState(pendingRating = rating, isUpdating = true)
            viewModelScope.launch {
                val video = observeVideo(id).first()
                val error = video?.let { (rateVideo(it, rating) as? AppResult.Failure)?.error }
                when {
                    // The player moved on to another video, which has its own state.
                    id != videoId.value -> Unit

                    error == CONSENT_REQUIRED && mayAskForConsent -> askForConsent(rating)

                    else -> requestState.value = RequestState(error = error)
                }
            }
        }

        private suspend fun askForConsent(rating: VideoRating) {
            when (val consent = accountConsent.consentScreen()) {
                is AppResult.Failure -> {
                    requestState.value = RequestState(error = AccountError.Auth(consent.error))
                }

                is AppResult.Success -> {
                    val sender = consent.value
                    if (sender == null) {
                        send(rating, mayAskForConsent = false)
                    } else {
                        requestState.value = RequestState(pendingRating = rating, consentScreen = sender)
                    }
                }
            }
        }

        private fun onConsentFinished(granted: Boolean) {
            val request = requestState.value
            if (!request.isAwaitingConsent) return
            val rating = request.pendingRating
            if (granted && rating != null) {
                send(rating, mayAskForConsent = false)
            } else {
                requestState.value = RequestState(error = AccountError.Auth(AuthError.Cancelled))
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            val CONSENT_REQUIRED = AccountError.Auth(AuthError.ConsentRequired)
        }
    }
