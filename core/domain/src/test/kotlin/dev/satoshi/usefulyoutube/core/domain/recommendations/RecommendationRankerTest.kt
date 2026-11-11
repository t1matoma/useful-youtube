package dev.satoshi.usefulyoutube.core.domain.recommendations

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DiscoveredVideo
import dev.satoshi.usefulyoutube.core.model.RatedVideo
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.core.model.WatchedVideo
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class RecommendationRankerTest {
    private val now = Instant.parse("2026-11-10T12:00:00Z")
    private val first = TestData.firstChannel
    private val second = TestData.secondChannel
    private val unknown = Channel(ChannelId("UCcccccccccccccccccccccc"), "Unknown channel")
    private val ranker = RecommendationRanker()

    private fun video(
        id: String,
        channel: Channel,
        age: Duration = 1.days,
    ) = TestData.video(id = id, channel = channel, publishedAt = now - age)

    private fun watched(vararg videos: Video) = videos.map { WatchedVideo(it, now) }

    private fun rank(
        feed: List<Video> = emptyList(),
        discovered: List<DiscoveredVideo> = emptyList(),
        history: List<WatchedVideo> = emptyList(),
        ratedVideos: List<RatedVideo> = emptyList(),
    ) = ranker
        .rank(RecommendationSignals(feed, discovered, history, ratedVideos), now)
        .map { it.id.value }

    @Test
    fun `without signals newer videos come first`() {
        val result = rank(feed = listOf(video("old", first, age = 10.days), video("new", second, age = 1.hours)))

        assertThat(result).containsExactly("new", "old")
    }

    @Test
    fun `a watched channel outranks a newer video of another channel`() {
        val history = watched(video("seen1", first), video("seen2", first), video("seen3", first))

        val result =
            rank(
                feed = listOf(video("other", second, age = 1.days), video("favorite", first, age = 2.days)),
                history = history,
            )

        assertThat(result).containsExactly("favorite", "other")
    }

    @Test
    fun `watched and rated videos are not recommended`() {
        val seen = video("seen", first)
        val liked = video("liked", first)

        val result =
            rank(
                feed = listOf(seen, liked, video("fresh", first)),
                history = watched(seen),
                ratedVideos = listOf(RatedVideo(liked.id, first.id, VideoRating.LIKE)),
            )

        assertThat(result).containsExactly("fresh")
    }

    @Test
    fun `likes weigh more than views and dislikes hide a channel`() {
        val result =
            rank(
                feed = listOf(video("a", first), video("b", second), video("c", unknown)),
                history = watched(video("seen", second)),
                ratedVideos =
                    listOf(
                        RatedVideo(TestData.video("x").id, first.id, VideoRating.LIKE),
                        RatedVideo(TestData.video("y").id, unknown.id, VideoRating.DISLIKE),
                    ),
            )

        assertThat(result).containsExactly("a", "b")
    }

    @Test
    fun `a featured channel inherits half of the affinity of the channel that features it`() {
        val history = watched(video("seen1", first), video("seen2", first))
        val discovered =
            listOf(
                DiscoveredVideo(video("featured", unknown), suggestedBy = first.id),
                DiscoveredVideo(video("orphan", Channel(ChannelId("UCdddddddddddddddddddddd"), "Orphan")), null),
            )

        val result = rank(feed = listOf(video("own", first), video("neutral", second)), discovered, history)

        assertThat(result).containsExactly("own", "featured", "neutral", "orphan")
    }

    @Test
    fun `a channel watched without a subscription counts with its own affinity`() {
        val history = watched(video("seen1", unknown), video("seen2", unknown))

        val result =
            rank(
                feed = listOf(video("subscribed", first)),
                discovered = listOf(DiscoveredVideo(video("more", unknown), suggestedBy = null)),
                history = history,
            )

        assertThat(result).containsExactly("more", "subscribed")
    }

    @Test
    fun `a video both in the feed and discovered is listed once`() {
        val discovered = listOf(DiscoveredVideo(video("both", first), suggestedBy = null))

        assertThat(rank(feed = listOf(video("both", first)), discovered = discovered)).containsExactly("both")
    }

    @Test
    fun `one channel takes at most three places and the list has at most a hundred videos`() {
        val manyOfOne = (1..5).map { video("first$it", first, age = it.hours) }
        val channels = (1..40).map { Channel(ChannelId("UC%022d".format(it)), "Channel $it") }
        val manyChannels = channels.flatMap { channel -> (1..3).map { video("${channel.id.value}-$it", channel) } }

        assertThat(rank(feed = manyOfOne)).containsExactly("first1", "first2", "first3")
        assertThat(rank(feed = manyChannels)).hasSize(100)
    }

    @Test
    fun `a video published in the future is as fresh as a new one`() {
        val result = rank(feed = listOf(video("premiere", first, age = (-2).days), video("old", second, age = 3.days)))

        assertThat(result).containsExactly("premiere", "old")
    }
}
