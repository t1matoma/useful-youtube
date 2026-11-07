package dev.satoshi.usefulyoutube.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
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

private const val COLLAPSED_DESCRIPTION_LINES = 3
private val ContentPadding = 16.dp
private val ItemSpacing = 8.dp

@Composable
fun PlayerRoute(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlayerScreen(state = state, onEvent = viewModel::onEvent, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onEvent: (PlayerUiEvent) -> Unit,
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
                VideoContent(state, onEvent, contentModifier)
            }
        }
    }
}

@Composable
private fun VideoContent(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        YouTubePlayer(videoId = state.video.id, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            VideoDetails(state, onEvent)
        }
    }
}

@Composable
private fun VideoDetails(
    state: PlayerUiState.Content,
    onEvent: (PlayerUiEvent) -> Unit,
) {
    val video = state.video
    val isDescriptionExpanded = state.isDescriptionExpanded
    Text(text = video.title, style = MaterialTheme.typography.titleLarge)
    Text(
        text = video.channelTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!state.isChannelSubscribed) {
        SubscribeButton(state, onEvent)
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
    state.subscribeError?.let { error ->
        Text(error.message(), color = MaterialTheme.colorScheme.error)
    }
}
