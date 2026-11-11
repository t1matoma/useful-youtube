package dev.satoshi.usefulyoutube.core.database

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.database.entity.DiscoveredVideoEntity
import dev.satoshi.usefulyoutube.core.database.entity.FeaturedChannelsEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoRatingEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoSnapshot
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoRating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RatingAndDiscoveryDaoTest : DatabaseTest() {
    private val ratingDao get() = database.videoRatingDao()
    private val discoveryDao get() = database.discoveryDao()
    private val watchable = setOf(VideoContentKind.REGULAR, VideoContentKind.LIVE_OR_PREMIERE)

    private fun discovered(
        videoId: String,
        channelId: String,
        publishedAt: Long = 0,
        kind: VideoContentKind = VideoContentKind.REGULAR,
        suggestedBy: String? = null,
    ) = DiscoveredVideoEntity(
        video =
            VideoSnapshot(
                videoId = videoId,
                channelId = channelId,
                channelTitle = "Channel $channelId",
                title = "Video $videoId",
                description = "",
                publishedAtEpochMillis = publishedAt,
                durationSeconds = 600,
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                contentKind = kind,
            ),
        suggestedBy = suggestedBy,
    )

    @Test
    fun `a rating is replaced and deleted`() =
        runTest {
            ratingDao.upsert(VideoRatingEntity("a", "UCa", VideoRating.LIKE))
            ratingDao.upsert(VideoRatingEntity("a", "UCa", VideoRating.DISLIKE))
            ratingDao.upsert(VideoRatingEntity("b", "UCb", VideoRating.LIKE))

            assertThat(ratingDao.observeRating("a").first()?.rating).isEqualTo(VideoRating.DISLIKE)
            assertThat(ratingDao.observeRatings().first().map { it.videoId }).containsExactlyInAnyOrder("a", "b")

            ratingDao.delete("a")

            assertThat(ratingDao.observeRating("a").first()).isNull()
        }

    @Test
    fun `discovered videos are newest first without shorts`() =
        runTest {
            discoveryDao.save(
                listOf(
                    discovered("old", "UCa", publishedAt = 1),
                    discovered("new", "UCa", publishedAt = 2, suggestedBy = "UCsource"),
                    discovered("short", "UCa", publishedAt = 3, kind = VideoContentKind.SHORT),
                ),
                channelIds = setOf("UCa"),
            )

            val videos = discoveryDao.observeVideos(watchable).first()

            assertThat(videos.map { it.video.videoId }).containsExactly("new", "old")
            assertThat(videos.first().suggestedBy).isEqualTo("UCsource")
            assertThat(discoveryDao.getVideoIds()).containsExactlyInAnyOrder("old", "new", "short")
            assertThat(
                discoveryDao
                    .observeVideo("old")
                    .first()
                    ?.video
                    ?.title,
            ).isEqualTo("Video old")
        }

    @Test
    fun `saving keeps stored videos and drops channels that left the list`() =
        runTest {
            discoveryDao.save(listOf(discovered("a1", "UCa"), discovered("b1", "UCb")), setOf("UCa", "UCb"))

            discoveryDao.save(listOf(discovered("a1", "UCa", publishedAt = 99), discovered("a2", "UCa")), setOf("UCa"))

            val videos = discoveryDao.observeVideos(watchable).first()
            assertThat(videos.map { it.video.videoId to it.video.publishedAtEpochMillis })
                .containsExactlyInAnyOrder("a1" to 0L, "a2" to 0L)

            discoveryDao.save(emptyList(), emptySet())

            assertThat(discoveryDao.getVideoIds()).isEmpty()
        }

    @Test
    fun `featured channels of a channel are replaced`() =
        runTest {
            assertThat(discoveryDao.getFeaturedChannels("UCsource")).isNull()

            discoveryDao.upsertFeaturedChannels(FeaturedChannelsEntity("UCsource", "UCa,UCb", fetchedAtEpochMillis = 1))
            discoveryDao.upsertFeaturedChannels(FeaturedChannelsEntity("UCsource", "", fetchedAtEpochMillis = 2))

            assertThat(discoveryDao.getFeaturedChannels("UCsource"))
                .isEqualTo(FeaturedChannelsEntity("UCsource", "", fetchedAtEpochMillis = 2))
        }
}
