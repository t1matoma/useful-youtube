package dev.satoshi.usefulyoutube.feature.feed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.model.DataError
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.error.message
import dev.satoshi.usefulyoutube.core.ui.theme.UsefulYouTubeTheme
import dev.satoshi.usefulyoutube.core.ui.video.VideoCard
import dev.satoshi.usefulyoutube.core.ui.R as CoreUiR

@Composable
fun FeedRoute(
    onVideoClick: (VideoId) -> Unit,
    onOpenChannels: () -> Unit,
    onImportSubscriptions: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeedScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onVideoClick = onVideoClick,
        onOpenChannels = onOpenChannels,
        onImportSubscriptions = onImportSubscriptions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    onEvent: (FeedUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    onOpenChannels: () -> Unit,
    onImportSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshErrorMessage = (state as? FeedUiState.Content)?.refreshError?.message()
    LaunchedEffect(refreshErrorMessage) {
        if (refreshErrorMessage != null) {
            snackbarHostState.showSnackbar(refreshErrorMessage)
            onEvent(FeedUiEvent.RefreshErrorShown)
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_feed_title)) },
                actions = {
                    IconButton(onClick = onOpenChannels) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.feature_feed_channels),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (state) {
            FeedUiState.Loading -> {
                LoadingContent(contentModifier)
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(FeedUiEvent.Refresh) },
                    modifier = contentModifier,
                ) {
                    FeedContent(state, onEvent, onVideoClick, onImportSubscriptions)
                }
            }
        }
    }
}

@Composable
private fun FeedContent(
    state: FeedUiState,
    onEvent: (FeedUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    onImportSubscriptions: () -> Unit,
) {
    // Empty and error states scroll so that pull-to-refresh works on them too.
    val scrollableModifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    when (state) {
        FeedUiState.Loading -> {}

        is FeedUiState.Empty -> {
            MessageContent(
                message = stringResource(R.string.feature_feed_empty),
                actionLabel = stringResource(R.string.feature_feed_import),
                onAction = onImportSubscriptions,
                modifier = scrollableModifier,
            )
        }

        is FeedUiState.Error -> {
            MessageContent(
                message = state.error.message(),
                actionLabel = stringResource(CoreUiR.string.core_ui_retry),
                onAction = { onEvent(FeedUiEvent.Refresh) },
                modifier = scrollableModifier,
            )
        }

        is FeedUiState.Content -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.videos, key = { it.id.value }) { video ->
                    VideoCard(video = video, onClick = { onVideoClick(video.id) })
                }
            }
        }
    }
}

private val FeedUiState.isRefreshing: Boolean
    get() =
        when (this) {
            FeedUiState.Loading -> true
            is FeedUiState.Empty -> isRefreshing
            is FeedUiState.Error -> isRefreshing
            is FeedUiState.Content -> isRefreshing
        }

@Preview
@Composable
private fun FeedScreenErrorPreview() {
    UsefulYouTubeTheme {
        FeedScreen(
            state = FeedUiState.Error(DataError.NoNetwork, isRefreshing = false),
            onEvent = {},
            onVideoClick = {},
            onOpenChannels = {},
            onImportSubscriptions = {},
        )
    }
}
