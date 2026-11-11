package dev.satoshi.usefulyoutube.feature.player.rating

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoRatingUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.ObserveVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RateVideoUseCase
import dev.satoshi.usefulyoutube.core.domain.usecase.RefreshVideoRatingUseCase
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.testing.MainDispatcherRule
import dev.satoshi.usefulyoutube.core.testing.collectInBackground
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeAccountAuthorizer
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeRecommendationRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRatingRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import dev.satoshi.usefulyoutube.feature.player.navigation.PlayerDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RatingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val video = TestData.video("abc")
    private val next = TestData.video("next")
    private val ratings = FakeVideoRatingRepository()
    private val authorizer = FakeAccountAuthorizer()
    private var consentScreen: AppResult<IntentSender?, AuthError> = AppResult.Success(null)

    // Created after MainDispatcherRule has replaced the main dispatcher, which viewModelScope captures.
    private val viewModel by lazy {
        RatingViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PlayerDestination.VIDEO_ID_ARGUMENT to video.id.value)),
            observeVideoRating = ObserveVideoRatingUseCase(ratings),
            observeVideo =
                ObserveVideoUseCase(
                    FakeVideoRepository(listOf(video, next)),
                    FakeSearchRepository(),
                    FakePlaylistRepository(),
                    FakeHistoryRepository(),
                    FakeRecommendationRepository(),
                ),
            refreshVideoRating = RefreshVideoRatingUseCase(authorizer, ratings),
            rateVideo = RateVideoUseCase(authorizer, ratings),
            accountConsent = { consentScreen },
        )
    }

    private fun intentSender(): IntentSender =
        PendingIntent
            .getActivity(RuntimeEnvironment.getApplication(), 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            .intentSender

    @Test
    fun `the rating of the account is shown when the video opens`() =
        runTest {
            ratings.remoteRatings[video.id] = VideoRating.LIKE
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState(rating = VideoRating.LIKE))
        }

    @Test
    fun `without a connected account the video opens unrated and without an error`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState())
        }

    @Test
    fun `like and dislike are sent and tapping the chosen one removes the rating`() =
        runTest {
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState(rating = VideoRating.LIKE))
            assertThat(ratings.remoteRatings[video.id]).isEqualTo(VideoRating.LIKE)

            viewModel.onEvent(RatingUiEvent.DislikeClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.rating).isEqualTo(VideoRating.DISLIKE)

            viewModel.onEvent(RatingUiEvent.DislikeClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.rating).isEqualTo(VideoRating.NONE)
            assertThat(ratings.remoteRatings[video.id]).isEqualTo(VideoRating.NONE)
            assertThat(ratings.currentRatedVideos).isEmpty()
        }

    @Test
    fun `a rating rejected by youtube shows the error and keeps the stored rating`() =
        runTest {
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()
            ratings.remoteError = DataError.QuotaExceeded

            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isEqualTo(RatingUiState(error = AccountError.Remote(DataError.QuotaExceeded)))

            ratings.remoteError = null
            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState(rating = VideoRating.LIKE))
        }

    @Test
    fun `the first like asks for consent and is sent once access is granted`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Success(intentSender())
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.consentScreen).isNotNull()
            assertThat(viewModel.uiState.value.rating).isEqualTo(VideoRating.LIKE)

            viewModel.onEvent(RatingUiEvent.ConsentLaunched)
            viewModel.onEvent(RatingUiEvent.DislikeClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.consentScreen).isNull()
            assertThat(viewModel.uiState.value.rating).isEqualTo(VideoRating.LIKE)

            authorizer.error = null
            viewModel.onEvent(RatingUiEvent.ConsentFinished(granted = true))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState(rating = VideoRating.LIKE))
            assertThat(ratings.remoteRatings[video.id]).isEqualTo(VideoRating.LIKE)
        }

    @Test
    fun `a declined consent is reported and nothing is rated`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            consentScreen = AppResult.Success(intentSender())
            collectInBackground(viewModel.uiState)
            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()
            viewModel.onEvent(RatingUiEvent.ConsentLaunched)

            viewModel.onEvent(RatingUiEvent.ConsentFinished(granted = false))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState(error = AccountError.Auth(AuthError.Cancelled)))
            assertThat(ratings.remoteRatings).isEmpty()
        }

    @Test
    fun `a consent result nobody waits for is ignored`() =
        runTest {
            collectInBackground(viewModel.uiState)
            advanceUntilIdle()

            viewModel.onEvent(RatingUiEvent.ConsentFinished(granted = false))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState())
        }

    @Test
    fun `consent that cannot be shown or is still required is an error, not a loop`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired
            collectInBackground(viewModel.uiState)

            consentScreen = AppResult.Failure(AuthError.Unavailable)
            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.error).isEqualTo(AccountError.Auth(AuthError.Unavailable))

            consentScreen = AppResult.Success(null)
            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.error).isEqualTo(AccountError.Auth(AuthError.ConsentRequired))
            assertThat(viewModel.uiState.value.isUpdating).isFalse()
        }

    @Test
    fun `another video of a playlist has its own rating`() =
        runTest {
            ratings.remoteRatings[next.id] = VideoRating.DISLIKE
            collectInBackground(viewModel.uiState)
            viewModel.onEvent(RatingUiEvent.LikeClicked)
            advanceUntilIdle()

            viewModel.onEvent(RatingUiEvent.VideoChanged(next.id))
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(RatingUiState(rating = VideoRating.DISLIKE))

            viewModel.onEvent(RatingUiEvent.VideoChanged(next.id))
            viewModel.onEvent(RatingUiEvent.DislikeClicked)
            advanceUntilIdle()

            assertThat(ratings.remoteRatings[next.id]).isEqualTo(VideoRating.NONE)
            assertThat(ratings.remoteRatings[video.id]).isEqualTo(VideoRating.LIKE)
        }
}
