package dev.satoshi.usefulyoutube.core.data.subscriptions

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.subscription
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.DataError
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NetworkAccountSubscriptionsReaderTest {
    private val testDispatcher = StandardTestDispatcher()
    private val network = FakeYouTubeNetworkDataSource()
    private val reader = NetworkAccountSubscriptionsReader(network, testDispatcher)

    @Test
    fun `subscriptions become channels without duplicates`() =
        runTest(testDispatcher) {
            network.subscriptionsByToken["token"] =
                AppResult.Success(
                    listOf(
                        subscription(channelId = "UCaaaaaaaaaaaaaaaaaaaaaa", title = "First"),
                        subscription(channelId = "UCbbbbbbbbbbbbbbbbbbbbbb", title = "Second"),
                        subscription(channelId = "UCaaaaaaaaaaaaaaaaaaaaaa", title = "First"),
                    ),
                )

            assertThat(reader.read("token")).isEqualTo(
                AppResult.Success(
                    listOf(
                        Channel(ChannelId("UCaaaaaaaaaaaaaaaaaaaaaa"), "First"),
                        Channel(ChannelId("UCbbbbbbbbbbbbbbbbbbbbbb"), "Second"),
                    ),
                ),
            )
        }

    @Test
    fun `network error is propagated`() =
        runTest(testDispatcher) {
            assertThat(reader.read("expired")).isEqualTo(AppResult.Failure(DataError.Unauthorized))
        }
}
