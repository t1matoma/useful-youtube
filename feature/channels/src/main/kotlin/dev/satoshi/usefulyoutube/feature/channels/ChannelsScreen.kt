package dev.satoshi.usefulyoutube.feature.channels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.model.Channel
import dev.satoshi.usefulyoutube.core.ui.component.LoadingContent
import dev.satoshi.usefulyoutube.core.ui.component.MessageContent
import dev.satoshi.usefulyoutube.core.ui.error.message

private val ContentPadding = 16.dp
private val ItemSpacing = 8.dp
private val ProgressSize = 24.dp

@Composable
fun ChannelsRoute(
    onNavigateBack: () -> Unit,
    onImportSubscriptions: () -> Unit,
    viewModel: ChannelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChannelsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onImportSubscriptions = onImportSubscriptions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    state: ChannelsUiState,
    onEvent: (ChannelsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onImportSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_channels_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_channels_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onImportSubscriptions) {
                        Text(stringResource(R.string.feature_channels_import))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AddChannelForm(state, onEvent)
            when {
                state.isLoading -> LoadingContent()
                state.channels.isEmpty() -> MessageContent(stringResource(R.string.feature_channels_empty))
                else -> ChannelList(state.channels, onEvent)
            }
        }
    }
    state.channelPendingRemoval?.let { channel -> RemoveChannelDialog(channel, onEvent) }
}

@Composable
private fun AddChannelForm(
    state: ChannelsUiState,
    onEvent: (ChannelsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(ContentPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        OutlinedTextField(
            value = state.input,
            onValueChange = { onEvent(ChannelsUiEvent.InputChanged(it)) },
            label = { Text(stringResource(R.string.feature_channels_input_label)) },
            singleLine = true,
            isError = state.addError != null,
            supportingText = state.addError?.let { error -> { Text(error.message()) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(ChannelsUiEvent.AddClicked) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ItemSpacing)) {
            Button(
                onClick = { onEvent(ChannelsUiEvent.AddClicked) },
                enabled = state.input.isNotBlank() && !state.isAdding,
            ) {
                Text(stringResource(R.string.feature_channels_add))
            }
            if (state.isAdding) CircularProgressIndicator(modifier = Modifier.size(ProgressSize))
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<Channel>,
    onEvent: (ChannelsUiEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(channels, key = { it.id.value }) { channel ->
            ListItem(
                headlineContent = { Text(channel.title.ifBlank { channel.id.value }) },
                trailingContent = {
                    IconButton(onClick = { onEvent(ChannelsUiEvent.RemoveRequested(channel)) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.feature_channels_remove, channel.title),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun RemoveChannelDialog(
    channel: Channel,
    onEvent: (ChannelsUiEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(ChannelsUiEvent.RemoveDismissed) },
        title = { Text(stringResource(R.string.feature_channels_remove_title)) },
        text = { Text(stringResource(R.string.feature_channels_remove_message, channel.title)) },
        confirmButton = {
            TextButton(onClick = { onEvent(ChannelsUiEvent.RemoveConfirmed) }) {
                Text(stringResource(R.string.feature_channels_remove_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ChannelsUiEvent.RemoveDismissed) }) {
                Text(stringResource(R.string.feature_channels_remove_cancel))
            }
        },
    )
}
