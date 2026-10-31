package dev.satoshi.usefulyoutube.core.domain.channel

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import org.junit.Test

class ChannelInputParserTest {
    private val parser = ChannelInputParser()
    private val channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw"

    private fun parsed(input: String) = parser.parse(input)

    @Test
    fun `raw channel id`() {
        assertThat(parsed(channelId)).isEqualTo(AppResult.Success(ChannelReference.ById(ChannelId(channelId))))
    }

    @Test
    fun `channel id with surrounding spaces`() {
        assertThat(parsed("  $channelId \n")).isEqualTo(AppResult.Success(ChannelReference.ById(ChannelId(channelId))))
    }

    @Test
    fun `channel links in different forms`() {
        val expected = AppResult.Success(ChannelReference.ById(ChannelId(channelId)))
        listOf(
            "https://www.youtube.com/channel/$channelId",
            "http://youtube.com/channel/$channelId/videos",
            "youtube.com/channel/$channelId?view_as=subscriber",
            "https://m.youtube.com/channel/$channelId",
            "HTTPS://WWW.YOUTUBE.COM/channel/$channelId",
        ).forEach { link -> assertThat(parsed(link)).isEqualTo(expected) }
    }

    @Test
    fun `handle and handle links`() {
        val expected = AppResult.Success(ChannelReference.ByHandle("Some.Channel_1"))
        listOf(
            "@Some.Channel_1",
            "https://www.youtube.com/@Some.Channel_1",
            "youtube.com/@Some.Channel_1/featured",
        ).forEach { input -> assertThat(parsed(input)).isEqualTo(expected) }
    }

    @Test
    fun `unsupported input is rejected`() {
        val expected = AppResult.Failure(ChannelError.InvalidInput)
        listOf(
            "",
            "hello",
            "UC123",
            "@ab",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://example.com/channel/$channelId",
            "uc_x5XG1OV2P6uZZ5FSM9Ttw",
        ).forEach { input -> assertThat(parsed(input)).isEqualTo(expected) }
    }
}
