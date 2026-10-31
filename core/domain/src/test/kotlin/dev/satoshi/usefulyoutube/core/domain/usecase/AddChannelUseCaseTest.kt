package dev.satoshi.usefulyoutube.core.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelInputParser
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeChannelRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddChannelUseCaseTest {
    private val repository = FakeChannelRepository(initialChannels = listOf(TestData.firstChannel))
    private val addChannel = AddChannelUseCase(ChannelInputParser(), repository)

    @Test
    fun `adds channel by link`() =
        runTest {
            val channel = TestData.secondChannel
            repository.remoteChannels[ChannelReference.ById(channel.id)] = channel

            val result = addChannel("https://www.youtube.com/channel/${channel.id.value}")

            assertThat(result).isEqualTo(AppResult.Success(channel))
            assertThat(repository.getChannels()).containsExactly(TestData.firstChannel, channel)
        }

    @Test
    fun `adds channel by handle`() =
        runTest {
            repository.remoteChannels[ChannelReference.ByHandle("second")] = TestData.secondChannel

            assertThat(addChannel("@second")).isEqualTo(AppResult.Success(TestData.secondChannel))
        }

    @Test
    fun `rejects invalid input`() =
        runTest {
            assertThat(addChannel("not a channel")).isEqualTo(AppResult.Failure(ChannelError.InvalidInput))
        }

    @Test
    fun `rejects already subscribed channel without remote call`() =
        runTest {
            repository.addChannelError = ChannelError.Remote(DataError.NoNetwork)

            val result = addChannel(TestData.firstChannel.id.value)

            assertThat(result).isEqualTo(AppResult.Failure(ChannelError.AlreadySubscribed))
        }

    @Test
    fun `propagates remote error`() =
        runTest {
            repository.addChannelError = ChannelError.Remote(DataError.NoNetwork)

            val result = addChannel(TestData.secondChannel.id.value)

            assertThat(result).isEqualTo(AppResult.Failure(ChannelError.Remote(DataError.NoNetwork)))
        }
}
