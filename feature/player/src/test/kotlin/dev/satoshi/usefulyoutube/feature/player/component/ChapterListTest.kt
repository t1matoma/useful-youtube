package dev.satoshi.usefulyoutube.feature.player.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.satoshi.usefulyoutube.core.model.Chapter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class ChapterListTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val chapters =
        listOf(
            Chapter("Intro", Duration.ZERO),
            Chapter("Main part", 2.minutes + 5.seconds),
            Chapter("Questions", 1.hours),
        )
    private val clicks = mutableListOf<Pair<Int, Chapter>>()

    private fun setContent(currentIndex: Int?) {
        composeRule.setContent {
            ChapterList(
                chapters = chapters,
                currentIndex = currentIndex,
                onChapterClick = { index, chapter -> clicks += index to chapter },
            )
        }
    }

    @Test
    fun `chapters show their start time and title and the current one is selected`() {
        setContent(currentIndex = 1)

        composeRule.onNodeWithText("0:00").assertIsDisplayed()
        composeRule.onNodeWithText("2:05").assertIsDisplayed()
        composeRule.onNodeWithText("Main part", useUnmergedTree = false).assertIsSelected()
        composeRule.onNodeWithText("Intro").assertIsNotSelected()
    }

    @Test
    fun `tapping a chapter reports it with its index`() {
        setContent(currentIndex = null)

        composeRule.onNodeWithText("Main part").performClick()

        assertThat(clicks).containsExactly(1 to chapters[1])
    }
}
