package dev.satoshi.usefulyoutube.feature.player.rating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.satoshi.usefulyoutube.core.model.VideoRating
import dev.satoshi.usefulyoutube.feature.player.R

private val ButtonSpacing = 8.dp
private val IconSize = 18.dp
private const val UPSIDE_DOWN = 180f

/** Like and dislike, as on YouTube: the chosen one is filled, and tapping it again removes the rating. */
@Composable
internal fun RatingButtons(
    state: RatingUiState,
    onEvent: (RatingUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(ButtonSpacing)) {
        RatingButton(
            checked = state.rating == VideoRating.LIKE,
            enabled = !state.isUpdating,
            label = stringResource(R.string.feature_player_like),
            onClick = { onEvent(RatingUiEvent.LikeClicked) },
        )
        RatingButton(
            checked = state.rating == VideoRating.DISLIKE,
            enabled = !state.isUpdating,
            label = stringResource(R.string.feature_player_dislike),
            onClick = { onEvent(RatingUiEvent.DislikeClicked) },
            // The core icon set has no thumb down.
            iconModifier = Modifier.rotate(UPSIDE_DOWN),
        )
    }
}

@Composable
private fun RatingButton(
    checked: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedIconToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = label },
        colors =
            IconButtonDefaults.outlinedIconToggleButtonColors(
                checkedContainerColor = colors.primary,
                checkedContentColor = colors.onPrimary,
            ),
    ) {
        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = iconModifier.size(IconSize))
    }
}
