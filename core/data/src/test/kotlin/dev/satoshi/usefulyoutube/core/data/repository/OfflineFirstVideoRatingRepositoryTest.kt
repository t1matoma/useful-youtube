package dev.satoshi.usefulyoutube.core.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeVideoRatingNetworkDataSource
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflineFirstVideoRatingRepositoryTest : RepositoryTest() {
    private val ratingNetwork = FakeVideoRatingNetworkDataSource()
    private val repository = OfflineFirstVideoRatingRepository(database.videoRatingDao(), ratingNetwork, testDispatcher)
    private val video = TestData.video("rated")

    @Test
    fun `a video is not rated until a rating is saved`() =
        runTest(testDispatcher) {
            assertThat(repository.observeRating(video.id).first()).isEqualTo(VideoRating.NONE)

            repository.saveRating(video, VideoRating.LIKE)

            assertThat(repository.observeRating(video.id).first()).isEqualTo(VideoRating.LIKE)
            assertThat(repository.observeRatedVideos().first())
                .containsExactly(RatedVideo(video.id, video.channelId, VideoRating.LIKE))
        }

    @Test
    fun `a rating is replaced and removing it forgets the video`() =
        runTest(testDispatcher) {
            repository.saveRating(video, VideoRating.LIKE)
            repository.saveRating(video, VideoRating.DISLIKE)

            assertThat(repository.observeRating(video.id).first()).isEqualTo(VideoRating.DISLIKE)

            repository.saveRating(video, VideoRating.NONE)

            assertThat(repository.observeRatedVideos().first()).isEmpty()
        }

    @Test
    fun `ratings of the account are mapped from the api values`() =
        runTest(testDispatcher) {
            ratingNetwork.ratings["liked"] = "like"
            ratingNetwork.ratings["disliked"] = "dislike"
            ratingNetwork.ratings["cleared"] = "none"

            val ratings =
                listOf("liked", "disliked", "cleared", "never-rated").map { id ->
                    repository.fetchRating("token", TestData.video(id).id)
                }

            assertThat(ratings).containsExactly(
                AppResult.Success(VideoRating.LIKE),
                AppResult.Success(VideoRating.DISLIKE),
                AppResult.Success(VideoRating.NONE),
                AppResult.Success(VideoRating.NONE),
            )
            assertThat(ratingNetwork.usedTokens.distinct()).containsExactly("token")
        }

    @Test
    fun `ratings are sent with the api values and do not touch the stored copy`() =
        runTest(testDispatcher) {
            repository.sendRating("token", video.id, VideoRating.LIKE)
            assertThat(ratingNetwork.ratings[video.id.value]).isEqualTo("like")
            repository.sendRating("token", video.id, VideoRating.DISLIKE)
            assertThat(ratingNetwork.ratings[video.id.value]).isEqualTo("dislike")
            repository.sendRating("token", video.id, VideoRating.NONE)
            assertThat(ratingNetwork.ratings[video.id.value]).isEqualTo("none")

            assertThat(repository.observeRatedVideos().first()).isEmpty()
        }

    @Test
    fun `network failures are reported`() =
        runTest(testDispatcher) {
            ratingNetwork.error = DataError.Unauthorized

            assertThat(repository.fetchRating("expired", video.id)).isEqualTo(AppResult.Failure(DataError.Unauthorized))
            assertThat(repository.sendRating("expired", video.id, VideoRating.LIKE))
                .isEqualTo(AppResult.Failure(DataError.Unauthorized))
        }
}
