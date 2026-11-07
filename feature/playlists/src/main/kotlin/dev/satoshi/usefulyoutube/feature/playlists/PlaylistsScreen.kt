package dev.satoshi.usefulyoutube.feature.playlists

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.model.Playlist
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.TextInputDialog
import dev.satoshi.usefulyoutube.core.ui.error.message
import dev.satoshi.usefulyoutube.core.ui.theme.UsefulYouTubeTheme
import dev.satoshi.usefulyoutube.core.ui.video.VideoThumbnail

private val ContentPadding = 16.dp
private val ItemSpacing = 8.dp
private val ThumbnailWidth = 96.dp

@Composable
fun PlaylistsRoute(
    onPlaylistClick: (PlaylistId) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlaylistsScreen(state = state, onEvent = viewModel::onEvent, onPlaylistClick = onPlaylistClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    state: PlaylistsUiState,
    onEvent: (PlaylistsUiEvent) -> Unit,
    onPlaylistClick: (PlaylistId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val consentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            onEvent(PlaylistsUiEvent.ConsentFinished(granted = result.resultCode == Activity.RESULT_OK))
        }
    state.consentScreen?.let { consentScreen ->
        LaunchedEffect(consentScreen) {
            consentLauncher.launch(IntentSenderRequest.Builder(consentScreen).build())
            onEvent(PlaylistsUiEvent.ConsentLaunched)
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_playlists_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(PlaylistsUiEvent.CreateClicked) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.feature_playlists_create))
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        if (state.isLoading) {
            LoadingContent(contentModifier)
        } else {
            PullToRefreshBox(
                isRefreshing = state.isSyncing,
                onRefresh = { onEvent(PlaylistsUiEvent.Refresh) },
                modifier = contentModifier,
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    playlistSection(R.string.feature_playlists_local, state.localPlaylists, onPlaylistClick)
                    if (state.localPlaylists.isEmpty()) {
                        item(key = "local-empty") { SectionMessage(stringResource(R.string.feature_playlists_empty)) }
                    }
                    playlistSection(R.string.feature_playlists_youtube, state.youtubePlaylists, onPlaylistClick)
                    item(key = "youtube-status") { YouTubeStatus(state, onEvent) }
                }
            }
        }
    }
    state.newPlaylistTitle?.let { title ->
        TextInputDialog(
            title = stringResource(R.string.feature_playlists_create),
            value = title,
            onValueChange = { onEvent(PlaylistsUiEvent.NewTitleChanged(it)) },
            confirmLabel = stringResource(R.string.feature_playlists_create_confirm),
            dismissLabel = stringResource(R.string.feature_playlists_cancel),
            onConfirm = { onEvent(PlaylistsUiEvent.CreateConfirmed) },
            onDismiss = { onEvent(PlaylistsUiEvent.CreateDismissed) },
        )
    }
}

/** Connection, emptiness and errors of the YouTube section, below its playlists. */
@Composable
private fun YouTubeStatus(
    state: PlaylistsUiState,
    onEvent: (PlaylistsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = ContentPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        state.syncError?.let { error ->
            Text(error.message(), color = MaterialTheme.colorScheme.error)
        }
        when {
            state.account == AccountState.NOT_CONNECTED -> {
                Text(stringResource(R.string.feature_playlists_connect_hint))
                Button(onClick = { onEvent(PlaylistsUiEvent.ConnectAccountClicked) }) {
                    Text(stringResource(R.string.feature_playlists_connect))
                }
            }

            state.account == AccountState.CONNECTED && state.youtubePlaylists.isEmpty() && !state.isSyncing -> {
                Text(stringResource(R.string.feature_playlists_youtube_empty))
            }

            else -> {}
        }
    }
}

@Composable
private fun SectionMessage(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = ContentPadding),
    )
}

private fun LazyListScope.playlistSection(
    titleRes: Int,
    playlists: List<Playlist>,
    onPlaylistClick: (PlaylistId) -> Unit,
) {
    item(key = "header-$titleRes") {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(ContentPadding),
        )
    }
    items(playlists, key = { it.id.value }) { playlist ->
        PlaylistRow(playlist, onClick = { onPlaylistClick(playlist.id) })
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(playlist.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(stringResource(R.string.feature_playlists_video_count, playlist.videoCount)) },
        leadingContent = { VideoThumbnail(url = playlist.thumbnailUrl, modifier = Modifier.width(ThumbnailWidth)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview
@Composable
private fun PlaylistsScreenNotConnectedPreview() {
    UsefulYouTubeTheme {
        PlaylistsScreen(
            state = PlaylistsUiState(isLoading = false, account = AccountState.NOT_CONNECTED),
            onEvent = {},
            onPlaylistClick = {},
        )
    }
}
