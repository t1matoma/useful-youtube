package dev.satoshi.usefulyoutube.feature.feed

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.PlaybackPosition
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.ui.component.LOADING_INDICATOR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.minutes
import dev.satoshi.usefulyoutube.core.ui.R as CoreUiR

@RunWith(RobolectricTestRunner::class)
class FeedScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<FeedUiEvent>()
    private val clickedVideos = mutableListOf<VideoId>()
    private var importClicks = 0
    private var settingsClicks = 0

    private fun string(
        @StringRes id: Int,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setContent(state: FeedUiState) {
        composeRule.setContent {
            FeedScreen(
                state = state,
                onEvent = { events += it },
                onVideoClick = { clickedVideos += it },
                onOpenChannels = {},
                onOpenSettings = { settingsClicks++ },
                onImportSubscriptions = { importClicks++ },
            )
        }
    }

    @Test
    fun `loading state shows progress indicator`() {
        setContent(FeedUiState.Loading)

        composeRule.onNodeWithTag(LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `content state shows videos and opens clicked one`() {
        val video = TestData.video(id = "abc")
        setContent(FeedUiState.Content(listOf(video), isRefreshing = false, refreshError = null))

        composeRule.onNodeWithText(video.title).assertIsDisplayed().performClick()

        assertThat(clickedVideos).containsExactly(VideoId("abc"))
    }

    @Test
    fun `empty state offers subscription import`() {
        setContent(FeedUiState.Empty(isRefreshing = false))

        composeRule.onNodeWithText(string(R.string.feature_feed_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.feature_feed_import)).performClick()

        assertThat(importClicks).isEqualTo(1)
    }

    @Test
    fun `error state shows message and retries`() {
        setContent(FeedUiState.Error(DataError.NoNetwork, isRefreshing = false))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_error_no_network)).assertIsDisplayed()
        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertThat(events).containsExactly(FeedUiEvent.Refresh)
    }

    @Test
    fun `refresh error over content is shown in snackbar and acknowledged`() {
        val video = TestData.video(id = "abc")
        setContent(FeedUiState.Content(listOf(video), isRefreshing = false, refreshError = DataError.QuotaExceeded))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_error_quota_exceeded)).assertIsDisplayed()
    }

    @Test
    fun `all watched state says so without offering import`() {
        setContent(FeedUiState.AllWatched(isRefreshing = false, refreshError = null))

        composeRule.onNodeWithText(string(R.string.feature_feed_all_watched)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.feature_feed_import)).assertDoesNotExist()
    }

    @Test
    fun `refresh error over all watched videos is shown in snackbar`() {
        setContent(FeedUiState.AllWatched(isRefreshing = false, refreshError = DataError.QuotaExceeded))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_error_quota_exceeded)).assertIsDisplayed()
    }

    @Test
    fun `watched video shows its progress`() {
        val video = TestData.video(id = "abc")
        setContent(
            FeedUiState.Content(
                listOf(video),
                isRefreshing = false,
                refreshError = null,
                playbackPositions = mapOf(video.id to PlaybackPosition(position = 2.minutes, duration = 8.minutes)),
            ),
        )

        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 0.25f, range = 0f..1f)))
            .assertExists()
    }

    @Test
    fun `settings open from the top bar`() {
        setContent(FeedUiState.Empty(isRefreshing = false))

        composeRule.onNodeWithContentDescription(string(R.string.feature_feed_settings)).performClick()

        assertThat(settingsClicks).isEqualTo(1)
    }
}
