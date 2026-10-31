package dev.satoshi.usefulyoutube.core.data.shorts

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NetworkShortsUrlProbeTest {
    @Test
    fun `delegates to network data source`() =
        runTest {
            val network = FakeYouTubeNetworkDataSource().apply { shortsUrls["v1"] = AppResult.Success(true) }

            assertThat(NetworkShortsUrlProbe(network).isShortsUrl(VideoId("v1"))).isEqualTo(AppResult.Success(true))
        }
}
