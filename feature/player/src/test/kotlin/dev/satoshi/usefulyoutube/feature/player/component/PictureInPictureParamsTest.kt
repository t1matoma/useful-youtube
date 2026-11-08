package dev.satoshi.usefulyoutube.feature.player.component

import android.content.Context
import android.graphics.Rect
import android.util.Rational
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.satoshi.usefulyoutube.feature.player.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PictureInPictureParamsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a playing video enters automatically and offers pause`() {
        val params = pictureInPictureParams(context, isPlaying = true, videoBounds = null)

        assertThat(params.isAutoEnterEnabled).isTrue()
        assertThat(params.actions.map { it.title.toString() })
            .containsExactly(context.getString(R.string.feature_player_pause))
    }

    @Test
    fun `a paused video does not enter automatically and offers play`() {
        val params = pictureInPictureParams(context, isPlaying = false, videoBounds = null)

        assertThat(params.isAutoEnterEnabled).isFalse()
        assertThat(params.actions.map { it.title.toString() })
            .containsExactly(context.getString(R.string.feature_player_play))
    }

    @Test
    fun `the window has the video aspect ratio and starts from the video`() {
        val bounds = Rect(0, 100, 1080, 708)

        val params = pictureInPictureParams(context, isPlaying = true, videoBounds = bounds)

        assertThat(params.aspectRatio).isEqualTo(Rational(16, 9))
        assertThat(params.sourceRectHint).isEqualTo(bounds)
    }
}
