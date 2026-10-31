package dev.satoshi.usefulyoutube.core.network.rss

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.network.Fixtures
import dev.satoshi.usefulyoutube.core.network.model.RssAuthorDto
import dev.satoshi.usefulyoutube.core.network.model.RssEntryDto
import dev.satoshi.usefulyoutube.core.network.model.RssMediaGroupDto
import dev.satoshi.usefulyoutube.core.network.model.RssThumbnailDto
import org.junit.Test

class RssFeedParserTest {
    private val parser = RssFeedParser()

    @Test
    fun `parses channel title and entries`() {
        val feed = parser.parse(Fixtures.read("channel_feed.xml"))

        assertThat(feed.title).isEqualTo("Google for Developers")
        assertThat(feed.entries).hasSize(2)
        assertThat(feed.entries.first()).isEqualTo(
            RssEntryDto(
                videoId = "aaaaaaaaaaa",
                channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw",
                title = "What's new in Android & Kotlin",
                published = "2026-10-30T16:00:06+00:00",
                author = RssAuthorDto(name = "Google for Developers"),
                mediaGroup =
                    RssMediaGroupDto(
                        thumbnail = RssThumbnailDto(url = "https://i2.ytimg.com/vi/aaaaaaaaaaa/hqdefault.jpg"),
                        description = "Line one\nLine two",
                    ),
            ),
        )
    }

    @Test
    fun `parses entry with empty description`() {
        val entry = parser.parse(Fixtures.read("channel_feed.xml")).entries[1]

        assertThat(entry.mediaGroup?.description).isEqualTo("")
    }

    @Test
    fun `parses feed without entries`() {
        val feed = parser.parse(Fixtures.read("empty_channel_feed.xml"))

        assertThat(feed.title).isEqualTo("Empty channel")
        assertThat(feed.entries).isEmpty()
    }

    @Test
    fun `malformed feed fails`() {
        assertFailure { parser.parse("<html><body>Not a feed</body></html>") }
    }
}
