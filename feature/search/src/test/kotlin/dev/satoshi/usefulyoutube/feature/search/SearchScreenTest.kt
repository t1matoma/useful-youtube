package dev.satoshi.usefulyoutube.feature.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import dev.satoshi.usefulyoutube.core.ui.component.LOADING_INDICATOR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import dev.satoshi.usefulyoutube.core.ui.R as CoreUiR

@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SearchUiEvent>()
    private val clickedVideos = mutableListOf<VideoId>()

    private fun string(
        @StringRes id: Int,
        vararg arguments: Any,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id, *arguments)

    private fun setContent(state: SearchUiState) {
        composeRule.setContent {
            SearchScreen(
                state = state,
                onEvent = { events += it },
                onNavigateBack = {},
                onVideoClick = { clickedVideos += it },
            )
        }
    }

    private fun results(
        vararg ids: String,
        canLoadMore: Boolean,
    ) = SearchResults("kotlin", SearchOrder.RELEVANCE, ids.map { TestData.video(id = it) }, canLoadMore)

    @Test
    fun `empty query shows recent queries that can be searched and deleted`() {
        setContent(SearchUiState(recentQueries = listOf("kotlin", "compose")))

        composeRule.onNodeWithText("compose").performClick()
        composeRule.onNodeWithContentDescription(string(R.string.feature_search_delete_recent, "kotlin")).performClick()

        assertThat(events).containsExactly(
            SearchUiEvent.RecentQuerySelected("compose"),
            SearchUiEvent.RecentQueryDeleted("kotlin"),
        )
    }

    @Test
    fun `empty query without recent queries shows a prompt`() {
        setContent(SearchUiState())

        composeRule.onNodeWithText(string(R.string.feature_search_prompt)).assertIsDisplayed()
    }

    @Test
    fun `search in progress shows progress indicator`() {
        setContent(SearchUiState(query = "kotlin", isSearching = true))

        composeRule.onNodeWithTag(LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `search error offers a retry`() {
        setContent(SearchUiState(query = "kotlin", searchError = DataError.QuotaExceeded))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_error_quota_exceeded)).assertIsDisplayed()
        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertThat(events).containsExactly(SearchUiEvent.SearchSubmitted)
    }

    @Test
    fun `search without results says nothing was found`() {
        setContent(SearchUiState(query = "kotlin", results = results(canLoadMore = false)))

        composeRule.onNodeWithText(string(R.string.feature_search_nothing_found)).assertIsDisplayed()
    }

    @Test
    fun `results open the clicked video and load more on request`() {
        val video = TestData.video(id = "abc")
        setContent(SearchUiState(query = "kotlin", results = results("abc", canLoadMore = true)))

        composeRule.onNodeWithText(video.title).performClick()
        composeRule.onNodeWithText(string(R.string.feature_search_load_more)).performClick()

        assertThat(clickedVideos).containsExactly(video.id)
        assertThat(events).containsExactly(SearchUiEvent.LoadMoreRequested)
    }

    @Test
    fun `order chip selects another order`() {
        setContent(SearchUiState(query = "kotlin"))

        composeRule.onNodeWithText(string(R.string.feature_search_order_date)).performClick()

        assertThat(events).containsExactly(SearchUiEvent.OrderSelected(SearchOrder.DATE))
    }
}
