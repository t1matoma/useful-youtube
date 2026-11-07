package dev.satoshi.usefulyoutube.feature.player.component

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.satoshi.usefulyoutube.feature.player.PlaybackQueue
import dev.satoshi.usefulyoutube.feature.player.PlayerUiEvent
import dev.satoshi.usefulyoutube.feature.player.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<PlayerUiEvent>()

    private fun string(
        @StringRes id: Int,
        vararg args: Any,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun setContent(queue: PlaybackQueue) {
        composeRule.setContent { QueueControls(queue = queue, onEvent = { events += it }) }
    }

    @Test
    fun `shows the position in the playlist`() {
        setContent(PlaybackQueue(playlistTitle = "Study", position = 3, size = 12))

        composeRule.onNodeWithText(string(R.string.feature_player_queue_position, "Study", 3, 12)).assertIsDisplayed()
    }

    @Test
    fun `first video has no previous and goes to the next one`() {
        setContent(PlaybackQueue(playlistTitle = "Study", position = 1, size = 2))

        composeRule.onNodeWithContentDescription(string(R.string.feature_player_previous)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(string(R.string.feature_player_next)).assertIsEnabled().performClick()

        assertThat(events).containsExactly(PlayerUiEvent.NextClicked)
    }

    @Test
    fun `last video has no next and goes to the previous one`() {
        setContent(PlaybackQueue(playlistTitle = "Study", position = 2, size = 2))

        composeRule.onNodeWithContentDescription(string(R.string.feature_player_next)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(string(R.string.feature_player_previous)).performClick()

        assertThat(events).containsExactly(PlayerUiEvent.PreviousClicked)
    }
}
