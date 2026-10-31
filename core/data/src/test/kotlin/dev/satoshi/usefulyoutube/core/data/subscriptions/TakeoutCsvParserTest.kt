package dev.satoshi.usefulyoutube.core.data.subscriptions

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.ImportError
import org.junit.Test

class TakeoutCsvParserTest {
    private val parser = TakeoutCsvParser()
    private val firstId = "UC_x5XG1OV2P6uZZ5FSM9Ttw"
    private val secondId = "UCVHFbqXqoYvEWM1Ddxl0QDg"

    @Test
    fun `parses takeout export with header`() {
        val csv =
            """
            Channel Id,Channel Url,Channel Title
            $firstId,http://www.youtube.com/channel/$firstId,Google for Developers
            $secondId,http://www.youtube.com/channel/$secondId,Android Developers
            """.trimIndent()

        assertThat(parser.parse(csv)).isEqualTo(
            AppResult.Success(
                listOf(
                    Channel(ChannelId(firstId), "Google for Developers"),
                    Channel(ChannelId(secondId), "Android Developers"),
                ),
            ),
        )
    }

    @Test
    fun `localized header, byte order mark and windows line endings are supported`() {
        val csv = "Идентификатор канала,URL канала,Название канала\r\n$firstId,url,Канал\r\n"

        assertThat(parser.parse(csv)).isEqualTo(AppResult.Success(listOf(Channel(ChannelId(firstId), "Канал"))))
    }

    @Test
    fun `quoted titles with commas and quotes are parsed`() {
        val csv = "Channel Id,Channel Url,Channel Title\n$firstId,url,\"Tom, Jerry and \"\"Friends\"\"\""

        assertThat(parser.parse(csv)).isEqualTo(
            AppResult.Success(listOf(Channel(ChannelId(firstId), "Tom, Jerry and \"Friends\""))),
        )
    }

    @Test
    fun `duplicates are removed`() {
        val csv = "$firstId,url,First\n$firstId,url,First again"

        assertThat(parser.parse(csv)).isEqualTo(AppResult.Success(listOf(Channel(ChannelId(firstId), "First"))))
    }

    @Test
    fun `missing title becomes empty`() {
        assertThat(parser.parse(firstId)).isEqualTo(AppResult.Success(listOf(Channel(ChannelId(firstId), ""))))
    }

    @Test
    fun `empty file and header only give no channels`() {
        assertThat(parser.parse("")).isEqualTo(AppResult.Success(emptyList<Channel>()))
        assertThat(
            parser.parse("Channel Id,Channel Url,Channel Title\n"),
        ).isEqualTo(AppResult.Success(emptyList<Channel>()))
    }

    @Test
    fun `file without channel ids is invalid`() {
        val csv = "name,age\nAlice,30\nBob,25"

        assertThat(parser.parse(csv)).isEqualTo(AppResult.Failure(ImportError.InvalidFormat))
    }
}
