package dev.satoshi.usefulyoutube.feature.player.rating

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.feature.player.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RatingButtonsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RatingUiEvent>()

    private fun string(
        @StringRes id: Int,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setContent(state: RatingUiState) {
        composeRule.setContent { RatingButtons(state = state, onEvent = { events += it }) }
    }

    private val like get() = composeRule.onNodeWithContentDescription(string(R.string.feature_player_like))
    private val dislike get() = composeRule.onNodeWithContentDescription(string(R.string.feature_player_dislike))

    @Test
    fun `a liked video has the like button on and both buttons report clicks`() {
        setContent(RatingUiState(rating = VideoRating.LIKE))

        like.assertIsOn().performClick()
        dislike.assertIsOff().performClick()

        assertThat(events).containsExactly(RatingUiEvent.LikeClicked, RatingUiEvent.DislikeClicked)
    }

    @Test
    fun `a disliked video has the dislike button on`() {
        setContent(RatingUiState(rating = VideoRating.DISLIKE))

        like.assertIsOff()
        dislike.assertIsOn()
    }

    @Test
    fun `buttons wait while a rating is sent`() {
        setContent(RatingUiState(rating = VideoRating.LIKE, isUpdating = true))

        like.assertIsNotEnabled()
        dislike.assertIsNotEnabled()
    }
}
