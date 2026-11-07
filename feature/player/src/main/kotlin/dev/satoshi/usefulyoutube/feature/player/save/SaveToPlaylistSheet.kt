package dev.satoshi.usefulyoutube.feature.player.save

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.satoshi.usefulyoutube.core.ui.component.TextInputDialog
import dev.satoshi.usefulyoutube.feature.player.R

private val HeaderPadding = 16.dp

/** The sheet and the "new playlist" dialog; shows nothing while the sheet is hidden. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaveToPlaylistSheet(
    state: SaveToPlaylistUiState,
    onEvent: (SaveToPlaylistUiEvent) -> Unit,
) {
    if (state.isSheetVisible) {
        ModalBottomSheet(onDismissRequest = { onEvent(SaveToPlaylistUiEvent.SheetDismissed) }) {
            LazyColumn {
                item {
                    Text(
                        text = stringResource(R.string.feature_player_save_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(HeaderPadding),
                    )
                }
                items(state.playlists, key = { it.id.value }) { option ->
                    val toggle = { onEvent(SaveToPlaylistUiEvent.PlaylistToggled(option.id, !option.containsVideo)) }
                    ListItem(
                        headlineContent = { Text(option.title) },
                        leadingContent = { Checkbox(checked = option.containsVideo, onCheckedChange = { toggle() }) },
                        modifier = Modifier.clickable(onClick = toggle),
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.feature_player_new_playlist)) },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier.clickable { onEvent(SaveToPlaylistUiEvent.NewPlaylistClicked) },
                    )
                }
            }
        }
    }
    state.newPlaylistTitle?.let { title ->
        TextInputDialog(
            title = stringResource(R.string.feature_player_new_playlist),
            value = title,
            onValueChange = { onEvent(SaveToPlaylistUiEvent.NewPlaylistTitleChanged(it)) },
            confirmLabel = stringResource(R.string.feature_player_new_playlist_confirm),
            dismissLabel = stringResource(R.string.feature_player_cancel),
            onConfirm = { onEvent(SaveToPlaylistUiEvent.NewPlaylistConfirmed) },
            onDismiss = { onEvent(SaveToPlaylistUiEvent.NewPlaylistDismissed) },
        )
    }
}
