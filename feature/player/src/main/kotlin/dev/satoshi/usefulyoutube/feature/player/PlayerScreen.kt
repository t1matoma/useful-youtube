package dev.satoshi.usefulyoutube.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.error.message
import dev.satoshi.usefulyoutube.core.ui.video.relativeDate
import dev.satoshi.usefulyoutube.feature.player.component.FullscreenPlayer
import dev.satoshi.usefulyoutube.feature.player.component.FullscreenVideo
import dev.satoshi.usefulyoutube.feature.player.component.QueueButton
import dev.satoshi.usefulyoutube.feature.player.component.QueueControls
import dev.satoshi.usefulyoutube.feature.player.component.YouTubePlayer
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistSheet
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistUiEvent
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistUiState
import dev.satoshi.usefulyoutube.feature.player.save.SaveToPlaylistViewModel

private const val COLLAPSED_DESCRIPTION_LINES = 3
private val ContentPadding = 20.dp
private val SectionSpacing = 18.dp
private val ItemSpacing = 12.dp
private val SmallSpacing = 2.dp
private val ButtonIconSize = 18.dp
private val ButtonIconSpacing = 8.dp
private val CardPadding = 16.dp
private val HeaderPadding = 4.dp

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

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onEvent: (PlayerUiEvent) -> Unit,
    saveState: SaveToPlaylistUiState,
    onSaveEvent: (SaveToPlaylistUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The fullscreen view belongs to the embedded player, so it is plain UI state and not part of the UiState.
    var fullscreenVideo by remember { mutableStateOf<FullscreenVideo?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.padding(HeaderPadding)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.feature_player_back),
                )
            }
            val contentModifier = Modifier.fillMaxWidth().weight(1f)
            when (state) {
                PlayerUiState.Loading -> {
                    LoadingContent(contentModifier)
                }

                PlayerUiState.NotFound -> {
                    MessageContent(
                        message = stringResource(R.string.feature_player_not_found),
                        modifier = contentModifier,
                    )
                }

                is PlayerUiState.Content -> {
                    Column(modifier = contentModifier) {
                        YouTubePlayer(
                            videoId = state.video.id,
                            onPlaybackStarted = { onEvent(PlayerUiEvent.PlaybackStarted) },
                            onVideoEnded = { onEvent(PlayerUiEvent.VideoEnded) },
                            onPlayingChange = { isPlaying = it },
                            onEnterFullscreen = { fullscreenVideo = it },
                            onExitFullscreen = { fullscreenVideo = null },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        VideoDetails(state, onEvent, onSaveEvent)
                    }
                }
            }
        }
        fullscreenVideo?.let { video ->
            // Previous and next are shown while the video is paused, together with the player's own controls.
            val queue = (state as? PlayerUiState.Content)?.queue?.takeUnless { isPlaying }
            FullscreenPlayer(
                video = video,
                sideControls =
                    queue?.let {
                        {
                            QueueButton(next = false, queue = it, onEvent = onEvent)
                            QueueButton(next = true, queue = it, onEvent = onEvent)
                        }
                    },
            )
        }
    }
    SaveToPlaylistSheet(saveState, onSaveEvent)
}

@Composable
private fun VideoDetails(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
    onSaveEvent: (SaveToPlaylistUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        Text(text = state.video.title, style = MaterialTheme.typography.titleLarge)
        ChannelRow(state, onEvent)
        state.subscribeError?.let { error ->
            Text(error.message(), color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(onClick = { onSaveEvent(SaveToPlaylistUiEvent.SaveClicked) }) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Text(stringResource(R.string.feature_player_save), modifier = Modifier.padding(start = ButtonIconSpacing))
        }
        state.queue?.let { queue -> QueueControls(queue, onEvent) }
        if (state.video.description.isNotBlank()) {
            DescriptionCard(state, onEvent)
        }
    }
}

@Composable
private fun ChannelRow(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ItemSpacing)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SmallSpacing)) {
            Text(
                text = state.video.channelTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = relativeDate(state.video),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.isChannelSubscribed) {
            val colors = MaterialTheme.colorScheme
            Button(
                onClick = { onEvent(PlayerUiEvent.SubscribeToChannel) },
                enabled = !state.isSubscribing,
                colors = ButtonDefaults.buttonColors(containerColor = colors.onSurface, contentColor = colors.surface),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
                Text(
                    stringResource(R.string.feature_player_subscribe),
                    modifier = Modifier.padding(start = ButtonIconSpacing),
                )
            }
        }
    }
}

@Composable
private fun DescriptionCard(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
) {
    val isExpanded = state.isDescriptionExpanded
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(start = CardPadding, top = CardPadding, end = CardPadding)) {
            SelectionContainer {
                Text(
                    text = state.video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { onEvent(PlayerUiEvent.DescriptionToggled) }) {
                Text(
                    stringResource(
                        if (isExpanded) R.string.feature_player_show_less else R.string.feature_player_show_more,
                    ),
                )
            }
        }
    }
}
