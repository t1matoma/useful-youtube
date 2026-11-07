package dev.satoshi.usefulyoutube.feature.player.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.satoshi.usefulyoutube.feature.player.PlaybackQueue
import dev.satoshi.usefulyoutube.feature.player.PlayerUiEvent
import dev.satoshi.usefulyoutube.feature.player.R

private val ControlsSpacing = 12.dp
private val LabelSpacing = 2.dp

/** "Playlist · 3 of 12" between the previous and next buttons, under a divider. */
@Composable
internal fun QueueControls(
    queue: PlaybackQueue,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ControlsSpacing)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ControlsSpacing),
        ) {
            QueueButton(next = false, queue = queue, onEvent = onEvent)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LabelSpacing),
            ) {
                Text(
                    text = stringResource(R.string.feature_player_queue_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                )
            }
            QueueButton(next = true, queue = queue, onEvent = onEvent)
        }
    }
}

/** The previous or the [next] video of the [queue]; disabled at the ends. */
@Composable
internal fun QueueButton(
    next: Boolean,
    queue: PlaybackQueue,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedIconButton(
        onClick = { onEvent(if (next) PlayerUiEvent.NextClicked else PlayerUiEvent.PreviousClicked) },
        enabled = if (next) queue.hasNext else queue.hasPrevious,
        colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier,
    ) {
        Icon(
            if (next) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription =
                stringResource(if (next) R.string.feature_player_next else R.string.feature_player_previous),
        )
    }
}
