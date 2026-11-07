package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlaylistUseCasesTest {
    private val repository = FakePlaylistRepository()
    private val video = TestData.video("v1")

    @Test
    fun `playlist is created with a trimmed title and an optional first video`() =
        runTest {
            val id = CreatePlaylistUseCase(repository)("  Watch later ", firstVideo = video)

            val created = repository.currentPlaylists.single()
            assertThat(created.playlist.id).isEqualTo(id)
            assertThat(created.playlist.title).isEqualTo("Watch later")
            assertThat(created.videos).containsExactly(video)
        }

    @Test
    fun `blank title creates nothing and renames nothing`() =
        runTest {
            val id = repository.givenPlaylist("Kept")

            assertThat(CreatePlaylistUseCase(repository)("   ")).isNull()
            RenamePlaylistUseCase(repository)(id, " ")

            assertThat(repository.currentPlaylists.map { it.playlist.title }).containsExactly("Kept")
        }

    @Test
    fun `playlist is renamed and deleted`() =
        runTest {
            val id = repository.givenPlaylist("Old")

            RenamePlaylistUseCase(repository)(id, " New ")
            assertThat(ObservePlaylistUseCase(repository)(id).first()?.playlist?.title).isEqualTo("New")

            DeletePlaylistUseCase(repository)(id)
            assertThat(ObservePlaylistsUseCase(repository)().first()).isEmpty()
        }

    @Test
    fun `ticking a playlist adds the video and unticking removes it`() =
        runTest {
            val id = repository.givenPlaylist("Later")
            val setVideoInPlaylist = SetVideoInPlaylistUseCase(repository)

            ObservePlaylistsContainingVideoUseCase(repository)(video.id).test {
                assertThat(awaitItem()).isEmpty()

                setVideoInPlaylist(id, video, inPlaylist = true)
                assertThat(awaitItem()).isEqualTo(setOf(id))

                setVideoInPlaylist(id, video, inPlaylist = false)
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `player finds videos saved in playlists`() =
        runTest {
            val saved = TestData.video("saved")
            repository.givenPlaylist("From YouTube", PlaylistSource.YOUTUBE, listOf(saved))
            val observeVideo =
                ObserveVideoUseCase(FakeVideoRepository(), FakeSearchRepository(), repository, FakeHistoryRepository())

            assertThat(observeVideo(VideoId("saved")).first()).isEqualTo(saved)
            assertThat(observeVideo(VideoId("missing")).first()).isNull()
        }
}
