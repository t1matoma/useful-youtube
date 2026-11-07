package dev.satoshi.usefulyoutube.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.model.SearchOrder
import dev.satoshi.usefulyoutube.core.model.SearchResults
import dev.satoshi.usefulyoutube.core.model.VideoId
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.error.message
import dev.satoshi.usefulyoutube.core.ui.theme.UsefulYouTubeTheme
import dev.satoshi.usefulyoutube.core.ui.video.VideoCard
import dev.satoshi.usefulyoutube.core.ui.R as CoreUiR

private val ContentPadding = 16.dp
private val ItemSpacing = 8.dp

@Composable
fun SearchRoute(
    onVideoClick: (VideoId) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onVideoClick = onVideoClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { QueryField(state.query, onEvent) })
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OrderSelector(state.order, enabled = !state.isSearching, onEvent = onEvent)
            SearchContent(state, onEvent, onVideoClick, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun QueryField(
    query: String,
    onEvent: (SearchUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (query.isEmpty()) focusRequester.requestFocus()
    }
    TextField(
        value = query,
        onValueChange = { onEvent(SearchUiEvent.QueryChanged(it)) },
        placeholder = { Text(stringResource(R.string.feature_search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    onEvent(SearchUiEvent.SearchSubmitted)
                },
            ),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onEvent(SearchUiEvent.QueryChanged("")) }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.feature_search_clear))
                }
            }
        },
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

@Composable
private fun OrderSelector(
    order: SearchOrder,
    enabled: Boolean,
    onEvent: (SearchUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = ContentPadding),
        horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        SearchOrder.entries.forEach { option ->
            FilterChip(
                selected = option == order,
                onClick = { onEvent(SearchUiEvent.OrderSelected(option)) },
                label = { Text(stringResource(option.label)) },
                enabled = enabled,
            )
        }
    }
}

private val SearchOrder.label: Int
    get() =
        when (this) {
            SearchOrder.RELEVANCE -> R.string.feature_search_order_relevance
            SearchOrder.DATE -> R.string.feature_search_order_date
        }

@Composable
private fun SearchContent(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier,
) {
    val results = state.results
    when {
        state.isSearching -> {
            LoadingContent(modifier)
        }

        state.query.isBlank() && state.recentQueries.isNotEmpty() -> {
            RecentQueries(state.recentQueries, onEvent, modifier)
        }

        state.searchError != null -> {
            MessageContent(
                message = state.searchError.message(),
                actionLabel = stringResource(CoreUiR.string.core_ui_retry),
                onAction = { onEvent(SearchUiEvent.SearchSubmitted) },
                modifier = modifier,
            )
        }

        results == null || state.query.isBlank() -> {
            MessageContent(message = stringResource(R.string.feature_search_prompt), modifier = modifier)
        }

        results.videos.isEmpty() && !results.canLoadMore -> {
            MessageContent(message = stringResource(R.string.feature_search_nothing_found), modifier = modifier)
        }

        else -> {
            ResultList(results, state, onEvent, onVideoClick, modifier)
        }
    }
}

@Composable
private fun RecentQueries(
    queries: List<String>,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = stringResource(R.string.feature_search_recent),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ContentPadding),
            )
        }
        items(queries, key = { it }) { query ->
            ListItem(
                headlineContent = { Text(query) },
                trailingContent = {
                    IconButton(onClick = { onEvent(SearchUiEvent.RecentQueryDeleted(query)) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.feature_search_delete_recent, query),
                        )
                    }
                },
                modifier = Modifier.clickable { onEvent(SearchUiEvent.RecentQuerySelected(query)) },
            )
        }
    }
}

@Composable
private fun ResultList(
    results: SearchResults,
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(results.videos, key = { it.id.value }) { video ->
            VideoCard(video = video, onClick = { onVideoClick(video.id) })
        }
        if (results.canLoadMore) {
            item { LoadMoreFooter(state, onEvent) }
        }
    }
}

@Composable
private fun LoadMoreFooter(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(ContentPadding), contentAlignment = Alignment.Center) {
        if (state.isLoadingMore) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ItemSpacing),
            ) {
                state.loadMoreError?.let { error ->
                    Text(error.message(), color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = { onEvent(SearchUiEvent.LoadMoreRequested) }) {
                    Text(stringResource(R.string.feature_search_load_more))
                }
            }
        }
    }
}

@Preview
@Composable
private fun SearchScreenPromptPreview() {
    UsefulYouTubeTheme {
        SearchScreen(state = SearchUiState(), onEvent = {}, onVideoClick = {})
    }
}
