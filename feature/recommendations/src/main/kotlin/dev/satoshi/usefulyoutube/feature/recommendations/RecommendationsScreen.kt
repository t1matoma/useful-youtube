package dev.satoshi.usefulyoutube.feature.recommendations

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.error.message
import dev.satoshi.usefulyoutube.core.ui.video.VideoCard
import dev.satoshi.usefulyoutube.core.ui.R as CoreUiR

@Composable
fun RecommendationsRoute(
    onVideoClick: (VideoId) -> Unit,
    viewModel: RecommendationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecommendationsScreen(state = state, onEvent = viewModel::onEvent, onVideoClick = onVideoClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    state: RecommendationsUiState,
    onEvent: (RecommendationsUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Error of the last refresh over stored videos, shown once.
    val refreshErrorMessage = (state as? RecommendationsUiState.Content)?.refreshError?.message()
    LaunchedEffect(refreshErrorMessage) {
        if (refreshErrorMessage != null) {
            snackbarHostState.showSnackbar(refreshErrorMessage)
            onEvent(RecommendationsUiEvent.RefreshErrorShown)
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_recommendations_title)) }) },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        if (state == RecommendationsUiState.Loading) {
            LoadingContent(contentModifier)
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(RecommendationsUiEvent.Refresh) },
                modifier = contentModifier,
            ) {
                RecommendationsContent(state, onEvent, onVideoClick)
            }
        }
    }
}

@Composable
private fun RecommendationsContent(
    state: RecommendationsUiState,
    onEvent: (RecommendationsUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
) {
    // Message states scroll so that pull-to-refresh works on them too.
    val scrollableModifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    when (state) {
        RecommendationsUiState.Loading -> {}

        is RecommendationsUiState.Empty -> {
            MessageContent(
                message = stringResource(R.string.feature_recommendations_empty),
                modifier = scrollableModifier,
            )
        }

        is RecommendationsUiState.Error -> {
            MessageContent(
                message = state.error.message(),
                actionLabel = stringResource(CoreUiR.string.core_ui_retry),
                onAction = { onEvent(RecommendationsUiEvent.Refresh) },
                modifier = scrollableModifier,
            )
        }

        is RecommendationsUiState.Content -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.videos, key = { it.id.value }) { video ->
                    VideoCard(video = video, onClick = { onVideoClick(video.id) })
                }
            }
        }
    }
}

private val RecommendationsUiState.isRefreshing: Boolean
    get() =
        when (this) {
            RecommendationsUiState.Loading -> true
            is RecommendationsUiState.Empty -> isRefreshing
            is RecommendationsUiState.Error -> isRefreshing
            is RecommendationsUiState.Content -> isRefreshing
        }
