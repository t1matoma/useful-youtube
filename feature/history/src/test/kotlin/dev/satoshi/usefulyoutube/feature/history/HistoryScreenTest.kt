package dev.satoshi.usefulyoutube.feature.history

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<HistoryUiEvent>()
    private val opened = mutableListOf<VideoId>()

    private fun string(
        @StringRes id: Int,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setContent(state: HistoryUiState) {
        composeRule.setContent {
            HistoryScreen(state = state, onEvent = { events += it }, onVideoClick = { opened += it })
        }
    }

    @Test
    fun `history shows days and opens, removes and clears videos`() {
        val video = TestData.video("abc")
        setContent(HistoryUiState(isLoading = false, days = listOf(HistoryDay(HistoryDayLabel.Today, listOf(video)))))

        composeRule.onNodeWithText(string(R.string.feature_history_today)).assertIsDisplayed()
        composeRule.onNodeWithText(video.title).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.feature_history_remove)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.feature_history_clear)).performClick()

        assertThat(opened).containsExactly(video.id)
        assertThat(events).containsExactly(HistoryUiEvent.VideoRemoved(video.id), HistoryUiEvent.ClearClicked)
    }

    @Test
    fun `empty history explains what appears here`() {
        setContent(HistoryUiState(isLoading = false))

        composeRule.onNodeWithText(string(R.string.feature_history_empty)).assertIsDisplayed()
    }

    @Test
    fun `clearing asks for confirmation`() {
        setContent(HistoryUiState(isLoading = false, isClearDialogVisible = true))

        composeRule.onNodeWithText(string(R.string.feature_history_clear_confirm)).performClick()

        assertThat(events).containsExactly(HistoryUiEvent.ClearConfirmed)
    }
}
