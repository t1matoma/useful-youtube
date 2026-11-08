package dev.satoshi.usefulyoutube.feature.playlists

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.model.PlaylistId
import dev.satoshi.usefulyoutube.core.model.PlaylistSource
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.component.TextInputDialog
import dev.satoshi.usefulyoutube.core.ui.video.VideoCard

private val PlayAllPadding = 12.dp

@Composable
fun PlaylistRoute(
    onNavigateBack: () -> Unit,
    onPlay: (VideoId, PlaylistId) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlaylistScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onPlay = onPlay,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    state: PlaylistUiState,
    onEvent: (PlaylistUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    /** Plays the playlist from the given video on. */
    onPlay: (VideoId, PlaylistId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state as? PlaylistUiState.Content
    val isEditable = content?.playlist?.playlist?.source == PlaylistSource.LOCAL
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        content
                            ?.playlist
                            ?.playlist
                            ?.title
                            .orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_playlists_back),
                        )
                    }
                },
                actions = {
                    if (isEditable) {
                        IconButton(onClick = { onEvent(PlaylistUiEvent.RenameClicked) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.feature_playlists_rename),
                            )
                        }
                        IconButton(onClick = { onEvent(PlaylistUiEvent.DeleteClicked) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.feature_playlists_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (state) {
            PlaylistUiState.Loading -> {
                LoadingContent(contentModifier)
            }

            PlaylistUiState.NotFound -> {
                MessageContent(
                    message = stringResource(R.string.feature_playlists_not_found),
                    modifier = contentModifier,
                )
            }

            is PlaylistUiState.Content -> {
                PlaylistVideos(state, isEditable, onEvent, onPlay, contentModifier)
            }
        }
    }
    content?.dialog?.let { dialog -> PlaylistDialogContent(dialog, content, onEvent, onNavigateBack) }
}

@Composable
private fun PlaylistVideos(
    state: PlaylistUiState.Content,
    isEditable: Boolean,
    onEvent: (PlaylistUiEvent) -> Unit,
    onPlay: (VideoId, PlaylistId) -> Unit,
    modifier: Modifier,
) {
    val playlistId = state.playlist.playlist.id
    if (state.playlist.videos.isEmpty()) {
        MessageContent(message = stringResource(R.string.feature_playlists_no_videos), modifier = modifier)
        return
    }
    LazyColumn(modifier = modifier) {
        item(key = "play-all") {
            Button(
                onClick = {
                    onPlay(
                        state.playlist.videos
                            .first()
                            .id,
                        playlistId,
                    )
                },
                modifier = Modifier.padding(PlayAllPadding),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.feature_playlists_play_all))
            }
        }
        items(state.playlist.videos, key = { it.id.value }) { video ->
            VideoCard(
                video = video,
                onClick = { onPlay(video.id, playlistId) },
                progress = state.playbackPositions[video.id]?.progress,
                action =
                    if (isEditable) {
                        {
                            IconButton(onClick = { onEvent(PlaylistUiEvent.VideoRemoved(video.id)) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.feature_playlists_remove_video),
                                )
                            }
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun PlaylistDialogContent(
    dialog: PlaylistDialog,
    content: PlaylistUiState.Content,
    onEvent: (PlaylistUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    when (dialog) {
        is PlaylistDialog.Rename -> {
            TextInputDialog(
                title = stringResource(R.string.feature_playlists_rename),
                value = dialog.title,
                onValueChange = { onEvent(PlaylistUiEvent.TitleChanged(it)) },
                confirmLabel = stringResource(R.string.feature_playlists_rename_confirm),
                dismissLabel = stringResource(R.string.feature_playlists_cancel),
                onConfirm = { onEvent(PlaylistUiEvent.RenameConfirmed) },
                onDismiss = { onEvent(PlaylistUiEvent.DialogDismissed) },
            )
        }

        PlaylistDialog.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { onEvent(PlaylistUiEvent.DialogDismissed) },
                text = {
                    Text(stringResource(R.string.feature_playlists_delete_question, content.playlist.playlist.title))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(PlaylistUiEvent.DeleteConfirmed)
                            onNavigateBack()
                        },
                    ) {
                        Text(stringResource(R.string.feature_playlists_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(PlaylistUiEvent.DialogDismissed) }) {
                        Text(stringResource(R.string.feature_playlists_cancel))
                    }
                },
            )
        }
    }
}
