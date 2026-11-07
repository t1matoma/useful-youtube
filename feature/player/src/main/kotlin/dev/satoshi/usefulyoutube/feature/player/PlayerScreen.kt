package dev.satoshi.usefulyoutube.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.error.message
import dev.satoshi.usefulyoutube.feature.player.component.YouTubePlayer
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistSheet
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistUiEvent
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistUiState
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistViewModel

private const val COLLAPSED_DESCRIPTION_LINES = 3
private val ContentPadding = 16.dp
private val ItemSpacing = 8.dp

@Composable
fun PlayerRoute(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    saveViewModel: SaveToPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by saveViewModel.uiState.collectAsStateWithLifecycle()
    // The sheet saves the video that is playing now, which changes while a playlist plays.
    (state as? PlayerUiState.Content)?.video?.id?.let { videoId ->
        LaunchedEffect(videoId) { saveViewModel.onEvent(SaveToPlaylistUiEvent.VideoChanged(videoId)) }
    }
    PlayerScreen(
        state = state,
        onEvent = viewModel::onEvent,
        saveState = saveState,
        onSaveEvent = saveViewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onEvent: (PlayerUiEvent) -> Unit,
    saveState: SaveToPlaylistUiState,
    onSaveEvent: (SaveToPlaylistUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_player_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (state) {
            PlayerUiState.Loading -> {
                LoadingContent(contentModifier)
            }

            PlayerUiState.NotFound -> {
                MessageContent(message = stringResource(R.string.feature_player_not_found), modifier = contentModifier)
            }

            is PlayerUiState.Content -> {
                VideoContent(state, onEvent, onSaveEvent, contentModifier)
            }
        }
    }
    SaveToPlaylistSheet(saveState, onSaveEvent)
}

@Composable
private fun VideoContent(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
    onSaveEvent: (SaveToPlaylistUiEvent) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        YouTubePlayer(
            videoId = state.video.id,
            onVideoEnded = { onEvent(PlayerUiEvent.VideoEnded) },
            modifier = Modifier.fillMaxWidth(),
        )
        state.queue?.let { queue -> QueueControls(queue, onEvent) }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            VideoDetails(state, onEvent, onSaveEvent)
        }
    }
}

@Composable
private fun VideoDetails(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
    onSaveEvent: (SaveToPlaylistUiEvent) -> Unit,
) {
    val video = state.video
    val isDescriptionExpanded = state.isDescriptionExpanded
    Text(text = video.title, style = MaterialTheme.typography.titleLarge)
    Text(
        text = video.channelTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(ItemSpacing)) {
        OutlinedButton(onClick = { onSaveEvent(SaveToPlaylistUiEvent.SaveClicked) }) {
            Text(stringResource(R.string.feature_player_save))
        }
        if (!state.isChannelSubscribed) {
            SubscribeButton(state, onEvent)
        }
    }
    state.subscribeError?.let { error ->
        Text(error.message(), color = MaterialTheme.colorScheme.error)
    }
    if (video.description.isNotBlank()) {
        SelectionContainer {
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = { onEvent(PlayerUiEvent.DescriptionToggled) }) {
            Text(
                stringResource(
                    if (isDescriptionExpanded) R.string.feature_player_show_less else R.string.feature_player_show_more,
                ),
            )
        }
    }
}

@Composable
private fun SubscribeButton(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
) {
    OutlinedButton(onClick = { onEvent(PlayerUiEvent.SubscribeToChannel) }, enabled = !state.isSubscribing) {
        Text(stringResource(R.string.feature_player_subscribe))
    }
}

@Composable
private fun QueueControls(
    queue: PlaybackQueue,
    onEvent: (PlayerUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                stringResource(
                    R.string.feature_player_queue_position,
                    queue.playlistTitle,
                    queue.position,
                    queue.size,
                ),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onEvent(PlayerUiEvent.PreviousClicked) }, enabled = queue.hasPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.feature_player_previous),
            )
        }
        IconButton(onClick = { onEvent(PlayerUiEvent.NextClicked) }, enabled = queue.hasNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.feature_player_next),
            )
        }
    }
}
