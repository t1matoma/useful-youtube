package dev.satoshi.usefulyoutube.feature.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import dev.satoshi.usefulyoutube.core.ui.component.LOADING_INDICATOR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SettingsUiEvent>()
    private var backClicks = 0

    private fun string(
        @StringRes id: Int,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setContent(state: SettingsUiState) {
        composeRule.setContent {
            SettingsScreen(state = state, onEvent = { events += it }, onNavigateBack = { backClicks++ })
        }
    }

    @Test
    fun `loading state shows progress indicator`() {
        setContent(SettingsUiState(isLoading = true))

        composeRule.onNodeWithTag(LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `enabled recommendations are turned off by tapping the row`() {
        setContent(SettingsUiState(isLoading = false, recommendationsEnabled = true))

        composeRule.onNodeWithText(string(R.string.feature_settings_recommendations)).assertIsDisplayed()
        composeRule.onNode(isToggleable()).assertIsOn().performClick()

        assertThat(events).containsExactly(SettingsUiEvent.RecommendationsToggled(enabled = false))
    }

    @Test
    fun `disabled recommendations are shown off and back navigates up`() {
        setContent(SettingsUiState(isLoading = false, recommendationsEnabled = false))

        composeRule.onNode(isToggleable()).assertIsOff()
        composeRule.onNodeWithContentDescription(string(R.string.feature_settings_back)).performClick()

        assertThat(backClicks).isEqualTo(1)
    }
}
