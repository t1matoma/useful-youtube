package dev.satoshi.usefulyoutube.core.database

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoSnapshot
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlaylistDaoTest : DatabaseTest() {
    private val playlistDao get() = database.playlistDao()
    private val itemDao get() = database.playlistItemDao()

    private fun snapshot(videoId: String) =
        VideoSnapshot(
            videoId = videoId,
            channelId = "UCzzzzzzzzzzzzzzzzzzzzzz",
            channelTitle = "Channel",
            title = "Video $videoId",
            description = "",
            publishedAtEpochMillis = 0,
            durationSeconds = 600,
            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            contentKind = VideoContentKind.REGULAR,
        )

    @Test
    fun `playlists are sorted by title with video count and first thumbnail`() =
        runTest {
            val music = playlistDao.insertPlaylist(PlaylistEntity(title = "music"))
            playlistDao.insertPlaylist(PlaylistEntity(title = "Kotlin"))
            itemDao.addToLocalPlaylist(music, snapshot("b"))
            itemDao.addToLocalPlaylist(music, snapshot("a"))

            val playlists = playlistDao.observePlaylists().first()

            assertThat(playlists.map { it.playlist.title }).containsExactly("Kotlin", "music")
            assertThat(playlists.map { it.videoCount }).containsExactly(0, 2)
            assertThat(playlists.map { it.thumbnailUrl })
                .containsExactly(null, "https://i.ytimg.com/vi/b/hqdefault.jpg")
        }

    @Test
    fun `videos are appended once in the order they were added`() =
        runTest {
            val id = playlistDao.insertPlaylist(PlaylistEntity(title = "Later"))

            listOf("c", "a", "c", "b").forEach { itemDao.addToLocalPlaylist(id, snapshot(it)) }

            val items = itemDao.observeItems(id).first()
            assertThat(items.map { it.video.videoId }).containsExactly("c", "a", "b")
            assertThat(items.map { it.position }).containsExactly(0, 1, 2)
            assertThat(itemDao.observePlaylistIdsContaining("a").first()).containsExactly(id)
            assertThat(
                itemDao
                    .observeItem("a")
                    .first()
                    ?.video
                    ?.title,
            ).isEqualTo("Video a")
        }

    @Test
    fun `local playlist can be renamed, emptied and deleted with its videos`() =
        runTest {
            val id = playlistDao.insertPlaylist(PlaylistEntity(title = "Old"))
            itemDao.addToLocalPlaylist(id, snapshot("a"))
            itemDao.addToLocalPlaylist(id, snapshot("b"))

            playlistDao.renameLocalPlaylist(id, "New")
            itemDao.removeFromLocalPlaylist(id, "a")
            assertThat(
                playlistDao
                    .observePlaylist(id)
                    .first()
                    ?.playlist
                    ?.title,
            ).isEqualTo("New")
            assertThat(itemDao.observeItems(id).first().map { it.video.videoId }).containsExactly("b")

            playlistDao.deleteLocalPlaylist(id)
            assertThat(playlistDao.observePlaylist(id).first()).isNull()
            assertThat(itemDao.observeItem("b").first()).isNull()
        }

    @Test
    fun `mirrored youtube playlists cannot be changed`() =
        runTest {
            val id = playlistDao.insertPlaylist(PlaylistEntity(title = "From YouTube", remoteId = "PLremote"))

            playlistDao.renameLocalPlaylist(id, "Renamed")
            itemDao.addToLocalPlaylist(id, snapshot("a"))
            playlistDao.deleteLocalPlaylist(id)

            assertThat(
                playlistDao
                    .observePlaylist(id)
                    .first()
                    ?.playlist
                    ?.title,
            ).isEqualTo("From YouTube")
            assertThat(itemDao.observeItems(id).first()).isEmpty()
        }
}
