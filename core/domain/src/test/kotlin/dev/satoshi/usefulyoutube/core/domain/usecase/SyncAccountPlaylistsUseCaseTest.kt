package dev.satoshi.usefulyoutube.core.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.domain.repository.AccountPlaylist
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsDetector
import dev.satoshi.usefulyoutube.core.domain.shorts.ShortsVerdict
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.AppResult
import dev.satoshi.usefulyoutube.core.model.AuthError
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeAccountAuthorizer
import dev.satoshi.usefulyoutube.core.testing.repository.FakeMirroredPlaylistRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SyncAccountPlaylistsUseCaseTest {
    private val authorizer = FakeAccountAuthorizer("stale", "fresh")
    private val mirror = FakeMirroredPlaylistRepository()
    private val classified = mutableListOf<VideoId>()
    private val detector =
        ShortsDetector { candidate ->
            classified += candidate.id
            if (candidate.duration!! <= 3.minutes) ShortsVerdict.SHORT else ShortsVerdict.NOT_SHORT
        }

    private fun sync(read: (String) -> AppResult<List<AccountPlaylist>, DataError>) =
        SyncAccountPlaylistsUseCase(authorizer, { read(it) }, mirror, ClassifyVideoUseCase(detector))

    private fun found(
        id: String,
        duration: Duration,
    ) = TestData.video(id = id, duration = duration, contentKind = VideoContentKind.UNCLASSIFIED)

    @Test
    fun `playlists are mirrored without shorts and keep their order`() =
        runTest {
            val playlists =
                listOf(
                    AccountPlaylist("PLa", "Talks", listOf(found("long", 20.minutes), found("short", 30.seconds))),
                    AccountPlaylist("PLb", "Live", listOf(found("live", Duration.ZERO), found("long", 20.minutes))),
                )

            val result = sync { AppResult.Success(playlists) }()

            assertThat(result).isEqualTo(AppResult.Success(Unit))
            val mirrored = mirror.mirroredPlaylists!!
            assertThat(mirrored.map { it.remoteId }).containsExactly("PLa", "PLb")
            assertThat(mirrored[0].videos.map { it.id.value to it.contentKind }).containsExactly(
                "long" to VideoContentKind.REGULAR,
            )
            assertThat(mirrored[1].videos.map { it.id.value to it.contentKind }).containsExactly(
                "live" to VideoContentKind.LIVE_OR_PREMIERE,
                "long" to VideoContentKind.REGULAR,
            )
            assertThat(classified).containsExactly(VideoId("long"), VideoId("short"))
        }

    @Test
    fun `videos known from the previous sync are not classified again`() =
        runTest {
            mirror.knownKinds[VideoId("known")] = VideoContentKind.REGULAR

            sync { AppResult.Success(listOf(AccountPlaylist("PLa", "Talks", listOf(found("known", 1.minutes))))) }()

            assertThat(classified).isEmpty()
            assertThat(
                mirror.mirroredPlaylists!!
                    .single()
                    .videos
                    .single()
                    .contentKind,
            ).isEqualTo(VideoContentKind.REGULAR)
        }

    @Test
    fun `account without consent keeps the mirror`() =
        runTest {
            authorizer.error = AuthError.ConsentRequired

            val result = sync { AppResult.Success(emptyList()) }()

            assertThat(result).isEqualTo(AppResult.Failure(AccountError.Auth(AuthError.ConsentRequired)))
            assertThat(mirror.mirroredPlaylists).isNull()
        }

    @Test
    fun `rejected token is replaced once and network errors are reported`() =
        runTest {
            val tokens = mutableListOf<String>()

            val result =
                sync { token ->
                    tokens += token
                    if (token ==
                        "stale"
                    ) {
                        AppResult.Failure(DataError.Unauthorized)
                    } else {
                        AppResult.Failure(DataError.NoNetwork)
                    }
                }()

            assertThat(tokens).containsExactly("stale", "fresh")
            assertThat(authorizer.clearedTokens).containsExactly("stale")
            assertThat(result).isEqualTo(AppResult.Failure(AccountError.Remote(DataError.NoNetwork)))
        }
}
