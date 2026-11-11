package dev.satoshi.usefulyoutube.feature.recommendations

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.ui.component.LOADING_INDICATOR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import dev.satoshi.usefulyoutube.core.ui.R as CoreUiR

@RunWith(RobolectricTestRunner::class)
class RecommendationsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RecommendationsUiEvent>()
    private val clickedVideos = mutableListOf<VideoId>()

    private fun string(
        @StringRes id: Int,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setContent(state: RecommendationsUiState) {
        composeRule.setContent {
            RecommendationsScreen(state = state, onEvent = { events += it }, onVideoClick = { clickedVideos += it })
        }
    }

    @Test
    fun `loading state shows progress indicator`() {
        setContent(RecommendationsUiState.Loading)

        composeRule.onNodeWithTag(LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `empty state explains where recommendations come from`() {
        setContent(RecommendationsUiState.Empty(isRefreshing = false))

        composeRule.onNodeWithText(string(R.string.feature_recommendations_empty)).assertIsDisplayed()
    }

    @Test
    fun `error state offers retry`() {
        setContent(RecommendationsUiState.Error(DataError.NoNetwork, isRefreshing = false))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_error_no_network)).assertIsDisplayed()
        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertThat(events).containsExactly(RecommendationsUiEvent.Refresh)
    }

    @Test
    fun `recommended videos open on click`() {
        val video = TestData.video("recommended")
        setContent(RecommendationsUiState.Content(listOf(video), isRefreshing = false, refreshError = null))

        composeRule.onNodeWithText(video.title).assertIsDisplayed().performClick()

        assertThat(clickedVideos).containsExactly(video.id)
    }

    @Test
    fun `refresh error over stored videos is shown once`() {
        val video = TestData.video("recommended")
        setContent(RecommendationsUiState.Content(listOf(video), isRefreshing = false, DataError.QuotaExceeded))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_error_quota_exceeded)).assertIsDisplayed()
    }
}
