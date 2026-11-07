package dev.satoshi.usefulyoutube.core.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistEntity
import dev.satoshi.usefulyoutube.core.model.ChannelId
import dev.satoshi.usefulyoutube.core.model.Playlist
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class OfflineFirstPlaylistRepositoryTest : RepositoryTest() {
    private val repository = OfflineFirstPlaylistRepository(database.playlistDao(), database.playlistItemDao())

    private fun video(id: String) =
        Video(
            id = VideoId(id),
            channelId = ChannelId("UCother"),
            channelTitle = "Other channel",
            title = "Title $id",
            description = "Description",
            publishedAt = Instant.parse("2026-10-30T16:00:06Z"),
            duration = 10.minutes,
            thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            contentKind = VideoContentKind.REGULAR,
        )

    @Test
    fun `app playlist keeps its videos in order`() =
        runTest(testDispatcher) {
            val id = repository.createPlaylist("Later")

            repository.addVideo(id, video("b"))
            repository.addVideo(id, video("a"))

            assertThat(repository.observePlaylist(id).first()).isEqualTo(
                PlaylistWithVideos(
                    Playlist(id, "Later", PlaylistSource.LOCAL, videoCount = 2, thumbnailUrl = video("b").thumbnailUrl),
                    listOf(video("b"), video("a")),
                ),
            )
            assertThat(repository.observePlaylistsContaining(VideoId("a")).first()).isEqualTo(setOf(id))
            assertThat(repository.observeVideo(VideoId("a")).first()).isEqualTo(video("a"))
        }

    @Test
    fun `app playlist is renamed, emptied and deleted`() =
        runTest(testDispatcher) {
            val id = repository.createPlaylist("Old")
            repository.addVideo(id, video("a"))

            repository.renamePlaylist(id, "New")
            repository.removeVideo(id, VideoId("a"))
            assertThat(repository.observePlaylists().first().map { it.title to it.videoCount })
                .containsExactly("New" to 0)

            repository.deletePlaylist(id)
            assertThat(repository.observePlaylists().first()).isEmpty()
            assertThat(repository.observePlaylist(id).first()).isNull()
        }

    @Test
    fun `mirrored playlist is reported as youtube and stays unchanged`() =
        runTest(testDispatcher) {
            val id = PlaylistId(database.playlistDao().insertPlaylist(PlaylistEntity(title = "Mix", remoteId = "PLx")))

            repository.renamePlaylist(id, "Renamed")
            repository.addVideo(id, video("a"))

            val playlist = repository.observePlaylists().first().single()
            assertThat(playlist.source).isEqualTo(PlaylistSource.YOUTUBE)
            assertThat(playlist.title).isEqualTo("Mix")
            assertThat(playlist.videoCount).isEqualTo(0)
        }
}
