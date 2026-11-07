package dev.satoshi.usefulyoutube.core.data.playlists

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.details
import dev.satoshi.usefulyoutube.core.data.testdoubles.FakeYouTubeNetworkDataSource.Companion.playlist
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.DataError
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NetworkAccountPlaylistsReaderTest {
    private val testDispatcher = StandardTestDispatcher()
    private val network = FakeYouTubeNetworkDataSource()
    private val reader = NetworkAccountPlaylistsReader(network, testDispatcher)

    @Test
    fun `playlists get their available videos with details requested once`() =
        runTest(testDispatcher) {
            network.playlistsResult = AppResult.Success(listOf(playlist("PLa", "Talks"), playlist("PLb", "Empty")))
            network.playlistVideoIds["PLa"] = AppResult.Success(listOf("b", "private", "a", "b"))
            network.videoDetails["a"] = details("a", "UCa", "PT10M")
            network.videoDetails["b"] = details("b", "UCb", "PT5M")

            val playlists = (reader.read("token") as AppResult.Success).value

            assertThat(playlists.map { it.remoteId to it.title }).containsExactly("PLa" to "Talks", "PLb" to "Empty")
            assertThat(playlists[0].videos.map { it.id.value }).containsExactly("b", "a", "b")
            assertThat(playlists[1].videos).isEmpty()
            assertThat(network.requestedDetailIds).containsExactly(listOf("b", "private", "a"))
        }

    @Test
    fun `account without playlists needs no details`() =
        runTest(testDispatcher) {
            assertThat(reader.read("token")).isEqualTo(AppResult.Success(emptyList()))
            assertThat(network.requestedDetailIds).isEmpty()
        }

    @Test
    fun `any failed request fails the read`() =
        runTest(testDispatcher) {
            network.playlistsResult = AppResult.Failure(DataError.Unauthorized)
            assertThat(reader.read("token")).isEqualTo(AppResult.Failure(DataError.Unauthorized))

            network.playlistsResult = AppResult.Success(listOf(playlist("PLa", "Talks")))
            network.playlistVideoIds["PLa"] = AppResult.Failure(DataError.NoNetwork)
            assertThat(reader.read("token")).isEqualTo(AppResult.Failure(DataError.NoNetwork))

            network.playlistVideoIds["PLa"] = AppResult.Success(listOf("a"))
            network.videoDetailsError = DataError.QuotaExceeded
            assertThat(reader.read("token")).isEqualTo(AppResult.Failure(DataError.QuotaExceeded))
        }
}
