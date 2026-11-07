package dev.satoshi.usefulyoutube.core.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylist
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class OfflineFirstMirroredPlaylistRepositoryTest : RepositoryTest() {
    private val now = Instant.parse("2026-11-07T12:00:00Z")
    private val mirror =
        OfflineFirstMirroredPlaylistRepository(
            database.mirroredPlaylistDao(),
            object : Clock {
                override fun now() = this@OfflineFirstMirroredPlaylistRepositoryTest.now
            },
        )
    private val playlists = OfflineFirstPlaylistRepository(database.playlistDao(), database.playlistItemDao())

    private fun video(id: String) =
        Video(
            id = VideoId(id),
            channelId = ChannelId("UCother"),
            channelTitle = "Other channel",
            title = "Title $id",
            description = "",
            publishedAt = Instant.parse("2026-10-30T16:00:06Z"),
            duration = 10.minutes,
            thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            contentKind = VideoContentKind.REGULAR,
        )

    @Test
    fun `mirrored playlists are shown as youtube playlists with their videos and kinds`() =
        runTest(testDispatcher) {
            mirror.replaceMirroredPlaylists(listOf(AccountPlaylist("PLa", "Talks", listOf(video("a"), video("b")))))

            val stored = playlists.observePlaylists().first().single()
            assertThat(stored.source).isEqualTo(PlaylistSource.YOUTUBE)
            assertThat(playlists.observePlaylist(stored.id).first()?.videos).isEqualTo(listOf(video("a"), video("b")))
            assertThat(mirror.getMirroredContentKinds().keys.map { it.value }).containsExactly("a", "b")
            val syncedAt =
                database
                    .playlistDao()
                    .observePlaylists()
                    .first()
                    .single()
                    .playlist.syncedAtEpochMillis
            assertThat(syncedAt).isEqualTo(now.toEpochMilliseconds())
        }
}
