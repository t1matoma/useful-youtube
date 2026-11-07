package dev.satoshi.usefulyoutube.feature.playlists

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import dev.satoshi.usefulyoutube.core.model.AccountError
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.Playlist
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.PlaylistWithVideos
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.testing.data.TestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import dev.satoshi.usefulyoutube.core.ui.R as CoreUiR

@RunWith(RobolectricTestRunner::class)
class PlaylistScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(
        @StringRes id: Int,
        vararg arguments: Any,
    ) = ApplicationProvider.getApplicationContext<Context>().getString(id, *arguments)

    private fun playlist(
        source: PlaylistSource,
        videoCount: Int = 0,
    ) = Playlist(PlaylistId(1), "Later", source, videoCount, thumbnailUrl = null)

    @Test
    fun `playlists screen opens the clicked playlist and creates new ones`() {
        val events = mutableListOf<PlaylistsUiEvent>()
        val opened = mutableListOf<PlaylistId>()
        composeRule.setContent {
            PlaylistsScreen(
                state = PlaylistsUiState(isLoading = false, localPlaylists = listOf(playlist(PlaylistSource.LOCAL, 3))),
                onEvent = { events += it },
                onPlaylistClick = { opened += it },
            )
        }

        composeRule.onNodeWithText(string(R.string.feature_playlists_video_count, 3)).assertIsDisplayed()
        composeRule.onNodeWithText("Later").performClick()
        composeRule.onNodeWithContentDescription(string(R.string.feature_playlists_create)).performClick()

        assertThat(opened).containsExactly(PlaylistId(1))
        assertThat(events).containsExactly(PlaylistsUiEvent.CreateClicked)
    }

    @Test
    fun `empty playlists screen explains how to start and offers to connect the account`() {
        val events = mutableListOf<PlaylistsUiEvent>()
        composeRule.setContent {
            PlaylistsScreen(
                state = PlaylistsUiState(isLoading = false, account = AccountState.NOT_CONNECTED),
                onEvent = { events += it },
                onPlaylistClick = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.feature_playlists_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.feature_playlists_connect)).performClick()

        assertThat(events).containsExactly(PlaylistsUiEvent.ConnectAccountClicked)
    }

    @Test
    fun `sync error is shown in the youtube section`() {
        composeRule.setContent {
            PlaylistsScreen(
                state =
                    PlaylistsUiState(
                        isLoading = false,
                        account = AccountState.CONNECTED,
                        syncError = AccountError.Remote(DataError.QuotaExceeded),
                    ),
                onEvent = {},
                onPlaylistClick = {},
            )
        }

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_error_quota_exceeded)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.feature_playlists_youtube_empty)).assertIsDisplayed()
    }

    @Test
    fun `app playlist offers editing and removing videos`() {
        val events = mutableListOf<PlaylistUiEvent>()
        val video = TestData.video("abc")
        composeRule.setContent {
            PlaylistScreen(
                state = PlaylistUiState.Content(PlaylistWithVideos(playlist(PlaylistSource.LOCAL, 1), listOf(video))),
                onEvent = { events += it },
                onNavigateBack = {},
                onVideoClick = {},
            )
        }

        composeRule.onNodeWithContentDescription(string(R.string.feature_playlists_remove_video)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.feature_playlists_rename)).performClick()

        assertThat(events).containsExactly(PlaylistUiEvent.VideoRemoved(video.id), PlaylistUiEvent.RenameClicked)
    }

    @Test
    fun `youtube playlist is read only`() {
        val clicked = mutableListOf<VideoId>()
        val video = TestData.video("abc")
        composeRule.setContent {
            PlaylistScreen(
                state = PlaylistUiState.Content(PlaylistWithVideos(playlist(PlaylistSource.YOUTUBE, 1), listOf(video))),
                onEvent = {},
                onNavigateBack = {},
                onVideoClick = { clicked += it },
            )
        }

        composeRule.onNodeWithText(video.title).performClick()

        assertThat(clicked).containsExactly(video.id)
        composeRule
            .onAllNodesWithContentDescription(string(R.string.feature_playlists_rename))
            .assertCountEquals(0)
    }
}
