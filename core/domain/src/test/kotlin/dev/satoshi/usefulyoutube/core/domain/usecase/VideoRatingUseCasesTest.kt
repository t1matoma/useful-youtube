package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeAccountAuthorizer
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRatingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VideoRatingUseCasesTest {
    private val video = TestData.video("rated")
    private val authorizer = FakeAccountAuthorizer()
    private val repository = FakeVideoRatingRepository()
    private val rateVideo = RateVideoUseCase(authorizer, repository)
    private val refreshRating = RefreshVideoRatingUseCase(authorizer, repository)

    @Test
    fun `a like is sent to the account and stored`() =
        runTest {
            val result = rateVideo(video, VideoRating.LIKE)

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            assertThat(repository.remoteRatings[video.id]).isEqualTo(VideoRating.LIKE)
            assertThat(repository.currentRatedVideos)
                .containsExactly(RatedVideo(video.id, video.channelId, VideoRating.LIKE))
            ObserveVideoRatingUseCase(repository)(video.id).test {
                assertThat(awaitItem()).isEqualTo(VideoRating.LIKE)
            }
        }

    @Test
    fun `removing a rating forgets it`() =
        runTest {
            rateVideo(video, VideoRating.DISLIKE)

            rateVideo(video, VideoRating.NONE)

            assertThat(repository.remoteRatings[video.id]).isEqualTo(VideoRating.NONE)
            assertThat(repository.currentRatedVideos).isEmpty()
        }

    @Test
    fun `a rating rejected by youtube is not stored`() =
        runTest {
            repository.remoteError = DataError.QuotaExceeded

            val result = rateVideo(video, VideoRating.LIKE)

            assertThat(result).isEqualTo(AppResult.Failure(AccountError.Remote(DataError.QuotaExceeded)))
            assertThat(repository.currentRatedVideos).isEmpty()
        }

    @Test
    fun `rating without a connected account asks for consent`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired

            val result = rateVideo(video, VideoRating.LIKE)

            assertThat(result).isEqualTo(AppResult.Failure(AccountError.Auth(AuthError.ConsentRequired)))
            assertThat(repository.currentRatedVideos).isEmpty()
        }

    @Test
    fun `an expired token is replaced once`() =
        runTest {
            val staleAuthorizer = FakeAccountAuthorizer("stale", "token")

            val result = RateVideoUseCase(staleAuthorizer, repository)(video, VideoRating.LIKE)

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            assertThat(staleAuthorizer.clearedTokens).containsExactly("stale")
        }

    @Test
    fun `refresh takes the rating from the account`() =
        runTest {
            repository.remoteRatings[video.id] = VideoRating.LIKE

            val result = refreshRating(video)

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            assertThat(repository.currentRatedVideos.map { it.rating }).containsExactly(VideoRating.LIKE)
        }

    @Test
    fun `refresh forgets a rating removed on youtube and keeps it on failure`() =
        runTest {
            repository.saveRating(video, VideoRating.LIKE)
            repository.remoteError = DataError.NoNetwork

            val failed = refreshRating(video)

            assertThat(failed).isEqualTo(AppResult.Failure(AccountError.Remote(DataError.NoNetwork)))
            assertThat(repository.currentRatedVideos.map { it.rating }).containsExactly(VideoRating.LIKE)

            repository.remoteError = null
            refreshRating(video)

            assertThat(repository.currentRatedVideos).isEmpty()
        }
}
