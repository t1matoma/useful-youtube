package dev.satoshi.usefulyoutube.core.database

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.database.entity.MirroredPlaylist
import dev.satoshi.usefulyoutube.core.database.entity.PlaylistEntity
import dev.satoshi.usefulyoutube.core.database.entity.StoredVideoKind
import dev.satoshi.usefulyoutube.core.database.entity.VideoSnapshot
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MirroredPlaylistDaoTest : DatabaseTest() {
    private val mirrorDao get() = database.mirroredPlaylistDao()
    private val playlistDao get() = database.playlistDao()

    private fun snapshot(
        videoId: String,
        kind: VideoContentKind = VideoContentKind.REGULAR,
    ) = VideoSnapshot(
        videoId = videoId,
        channelId = "UCzzzzzzzzzzzzzzzzzzzzzz",
        channelTitle = "Channel",
        title = "Video $videoId",
        description = "",
        publishedAtEpochMillis = 0,
        durationSeconds = 600,
        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        contentKind = kind,
    )

    @Test
    fun `sync keeps ids of remaining playlists, deletes missing ones and replaces videos`() =
        runTest {
            mirrorDao.replaceAll(
                listOf(
                    MirroredPlaylist("PLa", "Talks", listOf(snapshot("a"), snapshot("b"))),
                    MirroredPlaylist("PLb", "Gone", listOf(snapshot("c"))),
                ),
                syncedAtEpochMillis = 1,
            )
            val talksId =
                playlistDao
                    .observePlaylists()
                    .first()
                    .single { it.playlist.remoteId == "PLa" }
                    .playlist.id

            mirrorDao.replaceAll(
                listOf(MirroredPlaylist("PLa", "Kotlin talks", listOf(snapshot("b"), snapshot("d"), snapshot("b")))),
                syncedAtEpochMillis = 2,
            )

            val playlists = playlistDao.observePlaylists().first()
            assertThat(playlists.map { it.playlist }).containsExactly(
                PlaylistEntity(id = talksId, title = "Kotlin talks", remoteId = "PLa", syncedAtEpochMillis = 2),
            )
            assertThat(
                database
                    .playlistItemDao()
                    .observeItems(talksId)
                    .first()
                    .map { it.video.videoId },
            ).containsExactly("b", "d")
        }

    @Test
    fun `sync never touches app playlists`() =
        runTest {
            val localId = playlistDao.insertPlaylist(PlaylistEntity(title = "Mine"))
            database.playlistItemDao().addToLocalPlaylist(localId, snapshot("local"))

            mirrorDao.replaceAll(emptyList(), syncedAtEpochMillis = 1)

            assertThat(playlistDao.observePlaylists().first().map { it.playlist.title }).containsExactly("Mine")
            assertThat(mirrorDao.getStoredVideoKinds()).isEqualTo(emptyList<StoredVideoKind>())
        }

    @Test
    fun `stored kinds come from mirrored playlists only`() =
        runTest {
            val localId = playlistDao.insertPlaylist(PlaylistEntity(title = "Mine"))
            database.playlistItemDao().addToLocalPlaylist(localId, snapshot("local"))
            mirrorDao.replaceAll(
                listOf(MirroredPlaylist("PLa", "Live", listOf(snapshot("live", VideoContentKind.LIVE_OR_PREMIERE)))),
                syncedAtEpochMillis = 1,
            )

            assertThat(mirrorDao.getStoredVideoKinds())
                .containsExactly(StoredVideoKind("live", VideoContentKind.LIVE_OR_PREMIERE))
        }
}
