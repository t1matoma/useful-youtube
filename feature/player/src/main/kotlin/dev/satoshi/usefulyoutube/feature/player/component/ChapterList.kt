package dev.satoshi.usefulyoutube.feature.player.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.satoshi.usefulyoutube.core.model.Chapter
import dev.satoshi.usefulyoutube.core.ui.video.formatDuration
import dev.satoshi.usefulyoutube.feature.player.R

private const val TITLE_MAX_LINES = 2
private val ChapterWidth = 148.dp
private val ChapterSpacing = 8.dp
private val ChapterPadding = 12.dp
private val TextSpacing = 4.dp
private val HeaderSpacing = 10.dp
private val CurrentBorderWidth = 1.dp

/**
 * The chapters of the video in a row that scrolls sideways: the start time and the title. The chapter that is
 * playing is outlined and kept in view; tapping a chapter jumps to it.
 */
@Composable
internal fun ChapterList(
    chapters: List<Chapter>,
    currentIndex: Int?,
    onChapterClick: (index: Int, chapter: Chapter) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (currentIndex != null) listState.animateScrollToItem(currentIndex)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HeaderSpacing)) {
        Text(
            text = stringResource(R.string.feature_player_chapters),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(contentPadding),
        )
        LazyRow(
            state = listState,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(ChapterSpacing),
        ) {
            itemsIndexed(chapters) { index, chapter ->
                ChapterCard(
                    chapter = chapter,
                    isCurrent = index == currentIndex,
                    onClick = { onChapterClick(index, chapter) },
                )
            }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: Chapter,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.width(ChapterWidth).semantics { selected = isCurrent },
        color = if (isCurrent) colors.surfaceContainerHigh else colors.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = if (isCurrent) BorderStroke(CurrentBorderWidth, colors.primary) else null,
    ) {
        Column(modifier = Modifier.padding(ChapterPadding), verticalArrangement = Arrangement.spacedBy(TextSpacing)) {
            Text(
                text = formatDuration(chapter.start),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                minLines = TITLE_MAX_LINES,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
