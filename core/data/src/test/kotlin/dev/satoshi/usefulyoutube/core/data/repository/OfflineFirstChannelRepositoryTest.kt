package dev.satoshi.usefulyoutube.core.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.channel
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.feed
import dev.satoshi.usefulyoutube.core.domain.channel.ChannelReference
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelError
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflineFirstChannelRepositoryTest : RepositoryTest() {
    private val repository = OfflineFirstChannelRepository(database.channelDao(), network, testDispatcher)
    private val first = Channel(ChannelId("UCfirst"), "First")
    private val second = Channel(ChannelId("UCsecond"), "Second")

    @Test
    fun `imports only new channels`() =
        runTest(testDispatcher) {
            repository.importChannels(listOf(first))

            val imported = repository.importChannels(listOf(first, second))

            assertThat(imported).isEqualTo(1)
            assertThat(repository.getChannels()).containsExactly(first, second)
        }

    @Test
    fun `adds channel by id using title from its feed`() =
        runTest(testDispatcher) {
            network.feeds["UCfirst"] = feed("First from feed")

            val result = repository.addChannel(ChannelReference.ById(ChannelId("UCfirst")))

            assertThat(result).isEqualTo(AppResult.Success(Channel(ChannelId("UCfirst"), "First from feed")))
            assertThat(repository.getChannels()).containsExactly(Channel(ChannelId("UCfirst"), "First from feed"))
        }

    @Test
    fun `adding unknown channel id is not found`() =
        runTest(testDispatcher) {
            val result = repository.addChannel(ChannelReference.ById(ChannelId("UCmissing")))

            assertThat(result).isEqualTo(AppResult.Failure(ChannelError.NotFound))
        }

    @Test
    fun `network failure while adding by id is remote error`() =
        runTest(testDispatcher) {
            network.feeds["UCfirst"] = AppResult.Failure(DataError.NoNetwork)

            val result = repository.addChannel(ChannelReference.ById(ChannelId("UCfirst")))

            assertThat(result).isEqualTo(AppResult.Failure(ChannelError.Remote(DataError.NoNetwork)))
        }

    @Test
    fun `adds channel by handle`() =
        runTest(testDispatcher) {
            network.channelsByHandle["second"] = channel("UCsecond", "Second")

            assertThat(repository.addChannel(ChannelReference.ByHandle("second"))).isEqualTo(AppResult.Success(second))
        }

    @Test
    fun `unknown handle is not found`() =
        runTest(testDispatcher) {
            assertThat(repository.addChannel(ChannelReference.ByHandle("nobody")))
                .isEqualTo(AppResult.Failure(ChannelError.NotFound))
        }

    @Test
    fun `handle lookup failure is remote error`() =
        runTest(testDispatcher) {
            network.handleLookupError = DataError.QuotaExceeded

            assertThat(repository.addChannel(ChannelReference.ByHandle("second")))
                .isEqualTo(AppResult.Failure(ChannelError.Remote(DataError.QuotaExceeded)))
        }

    @Test
    fun `adding existing channel by handle is already subscribed`() =
        runTest(testDispatcher) {
            repository.importChannels(listOf(second))
            network.channelsByHandle["second"] = channel("UCsecond", "Second")

            assertThat(repository.addChannel(ChannelReference.ByHandle("second")))
                .isEqualTo(AppResult.Failure(ChannelError.AlreadySubscribed))
        }

    @Test
    fun `removed channel disappears from observed list`() =
        runTest(testDispatcher) {
            repository.importChannels(listOf(first, second))

            repository.observeChannels().test {
                assertThat(awaitItem()).containsExactly(first, second)

                repository.removeChannel(first.id)
                assertThat(awaitItem()).containsExactly(second)

                repository.removeChannel(second.id)
                assertThat(awaitItem()).isEmpty()
            }
        }
}
