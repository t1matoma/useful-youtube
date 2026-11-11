package dev.satoshi.usefulyoutube.core.domain.usecase

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import dev.satoshi.usefulyoutube.core.model.Chapter
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.testing.repository.FakeHistoryRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakePlaylistRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeRecommendationRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeSearchRepository
import dev.satoshi.usefulyoutube.core.testing.repository.FakeVideoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ParseChaptersUseCaseTest {
    private val parseChapters = ParseChaptersUseCase()

    @Test
    fun `timestamps at the start of lines become chapters`() {
        val description =
            """
            A talk about coroutines.

            0:00 Intro
            1:30 Structured concurrency
            12:05 Questions
            1:02:03 Outro

            Subscribe!
            """.trimIndent()

        assertThat(parseChapters(description)).containsExactly(
            Chapter("Intro", Duration.ZERO),
            Chapter("Structured concurrency", 1.minutes + 30.seconds),
            Chapter("Questions", 12.minutes + 5.seconds),
            Chapter("Outro", 1.hours + 2.minutes + 3.seconds),
        )
    }

    @Test
    fun `decorations and separators around timestamps are dropped`() {
        val description =
            """
            [00:00] - Вступление
            (02:10) — Основная часть (live)
            • 05:00 | Итоги
            07:00: Вопросы
            """.trimIndent()

        assertThat(parseChapters(description).map { it.title }).containsExactly(
            "Вступление",
            "Основная часть (live)",
            "Итоги",
            "Вопросы",
        )
    }

    @Test
    fun `timestamps at the end of lines and ranges are supported`() {
        val trailing = "Intro - 0:00\nPart 2 (3:00)\nEnd 5:00"
        val ranges = "0:00 - 3:00 Intro\n3:00 - 5:00 Middle\n5:00 - 9:00 End"

        assertThat(parseChapters(trailing)).containsExactly(
            Chapter("Intro", Duration.ZERO),
            Chapter("Part 2", 3.minutes),
            Chapter("End", 5.minutes),
        )
        assertThat(parseChapters(ranges).map { it.title }).containsExactly("Intro", "Middle", "End")
    }

    @Test
    fun `a list that does not start at zero has no chapters`() {
        assertThat(parseChapters("0:30 First\n1:00 Second\n2:00 Third")).isEmpty()
    }

    @Test
    fun `fewer than three chapters are not a chapter list`() {
        assertThat(parseChapters("0:00 Intro\n1:00 The rest")).isEmpty()
        assertThat(parseChapters("No timestamps here")).isEmpty()
    }

    @Test
    fun `lines before the first chapter and timestamps that go back are ignored`() {
        val description =
            """
            Premiere at 18:00 Moscow time
            0:00 Intro
            2:00 Topic
            1:00 see also the part about setup
            4:00 Outro
            The meeting was at 3:15 pm that day
            """.trimIndent()

        assertThat(parseChapters(description).map { it.start }).containsExactly(Duration.ZERO, 2.minutes, 4.minutes)
    }

    @Test
    fun `invalid timestamps and lines without a title are skipped`() {
        val description = "0:00 Intro\n1:75 Not a time\n2:00\n3:00 Middle\n4:00 End"

        assertThat(parseChapters(description).map { it.title }).containsExactly("Intro", "Middle", "End")
    }

    @Test
    fun `timestamps after the end of the video are ignored`() {
        val description = "0:00 Intro\n1:00 Middle\n2:00 End\n59:00 Bonus"

        assertThat(parseChapters(description, duration = 10.minutes).map { it.title })
            .containsExactly("Intro", "Middle", "End")
        assertThat(parseChapters(description, duration = Duration.ZERO).map { it.title })
            .containsExactly("Intro", "Middle", "End", "Bonus")
    }

    @Test
    fun `a stored video is observed with its chapters`() =
        runTest {
            val video = TestData.video("chaptered").copy(description = "0:00 Intro\n1:00 Middle\n2:00 End")
            val observeVideo =
                ObserveVideoWithChaptersUseCase(
                    ObserveVideoUseCase(
                        FakeVideoRepository(listOf(video)),
                        FakeSearchRepository(),
                        FakePlaylistRepository(),
                        FakeHistoryRepository(),
                        FakeRecommendationRepository(),
                    ),
                    parseChapters,
                )

            observeVideo(video.id).test {
                val loaded = awaitItem()
                assertThat(loaded?.video).isEqualTo(video)
                assertThat(loaded?.chapters?.map { it.title }).isEqualTo(listOf("Intro", "Middle", "End"))
            }
            observeVideo(VideoId("missing")).test { assertThat(awaitItem()).isNull() }
        }
}
