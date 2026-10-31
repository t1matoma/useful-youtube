package dev.satoshi.usefulyoutube.feature.subscriptionimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.satoshi.usefulyoutube.core.ui.error.message

private val ContentPadding = 24.dp
private val ItemSpacing = 16.dp
private val CSV_MIME_TYPES = arrayOf("text/*", "application/vnd.ms-excel", "application/octet-stream")

@Composable
fun ImportRoute(
    onNavigateBack: () -> Unit,
    onOpenFeed: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImportScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onOpenFeed = onOpenFeed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ImportUiState,
    onEvent: (ImportUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenFeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onEvent(ImportUiEvent.FileSelected(it.toString())) }
        }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_import_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_import_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            Text(stringResource(R.string.feature_import_instructions), style = MaterialTheme.typography.bodyLarge)
            when (state) {
                ImportUiState.Idle -> {}

                ImportUiState.Importing -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.feature_import_in_progress))
                }

                is ImportUiState.Imported -> {
                    Text(
                        stringResource(R.string.feature_import_success, state.newChannelCount),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(onClick = onOpenFeed) { Text(stringResource(R.string.feature_import_open_feed)) }
                }

                is ImportUiState.Failed -> {
                    Text(state.error.message(), color = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedButton(
                onClick = { filePicker.launch(CSV_MIME_TYPES) },
                enabled = state != ImportUiState.Importing,
            ) {
                Text(stringResource(R.string.feature_import_choose_file))
            }
        }
    }
}
