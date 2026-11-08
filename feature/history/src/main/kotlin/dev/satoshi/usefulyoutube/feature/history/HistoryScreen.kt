package dev.satoshi.usefulyoutube.feature.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.theme.UsefulYouTubeTheme
import dev.satoshi.usefulyoutube.core.ui.video.VideoCard
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val ContentPadding = 16.dp

@Composable
fun HistoryRoute(
    onVideoClick: (VideoId) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onEvent = viewModel::onEvent, onVideoClick = onVideoClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_history_title)) },
                actions = {
                    if (state.days.isNotEmpty()) {
                        IconButton(onClick = { onEvent(HistoryUiEvent.ClearClicked) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.feature_history_clear),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            state.isLoading -> {
                LoadingContent(contentModifier)
            }

            state.days.isEmpty() -> {
                MessageContent(message = stringResource(R.string.feature_history_empty), modifier = contentModifier)
            }

            else -> {
                HistoryList(state, onEvent, onVideoClick, contentModifier)
            }
        }
    }
    if (state.isClearDialogVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(HistoryUiEvent.ClearDismissed) },
            text = { Text(stringResource(R.string.feature_history_clear_question)) },
            confirmButton = {
                TextButton(onClick = { onEvent(HistoryUiEvent.ClearConfirmed) }) {
                    Text(stringResource(R.string.feature_history_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(HistoryUiEvent.ClearDismissed) }) {
                    Text(stringResource(R.string.feature_history_cancel))
                }
            },
        )
    }
}

@Composable
private fun HistoryList(
    state: HistoryUiState,
    onEvent: (HistoryUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    LazyColumn(modifier = modifier) {
        state.days.forEach { day ->
            item(key = "day-${day.day}") {
                Text(
                    text = day.day.text(dateFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ContentPadding),
                )
            }
            items(day.videos, key = { it.id.value }) { video ->
                VideoCard(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                    progress = state.playbackPositions[video.id]?.progress,
                    action = {
                        IconButton(onClick = { onEvent(HistoryUiEvent.VideoRemoved(video.id)) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.feature_history_remove),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryDayLabel.text(dateFormatter: DateTimeFormatter): String =
    when (this) {
        HistoryDayLabel.Today -> stringResource(R.string.feature_history_today)
        HistoryDayLabel.Yesterday -> stringResource(R.string.feature_history_yesterday)
        is HistoryDayLabel.Date -> date.format(dateFormatter)
    }

@Preview
@Composable
private fun HistoryScreenEmptyPreview() {
    UsefulYouTubeTheme {
        HistoryScreen(state = HistoryUiState(isLoading = false), onEvent = {}, onVideoClick = {})
    }
}
