package dev.satoshi.usefulyoutube.core.ui.video

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.satoshi.usefulyoutube.core.model.Video
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import dev.satoshi.usefulyoutube.core.ui.R
import kotlin.time.Clock

private const val THUMBNAIL_ASPECT_RATIO = 16f / 9f
private const val BADGE_BACKGROUND_ALPHA = 0.85f
private const val TITLE_MAX_LINES = 2
private const val PROGRESS_TRACK_ALPHA = 0.35f
private val CardBottomPadding = 20.dp
private val TextPadding = 16.dp
private val TextTopPadding = 12.dp
private val TextSpacing = 4.dp
private val BadgeMargin = 12.dp
private val BadgeHorizontalPadding = 8.dp
private val BadgeVerticalPadding = 3.dp
private val LiveDotSize = 6.dp
private val LiveDotSpacing = 6.dp
private val ThumbnailCornerRadius = 10.dp
private val ProgressBarHeight = 3.dp

/**
 * A video with an edge-to-edge thumbnail, the title and "channel · date" below it. [progress] is the watched part
 * of the video, from 0 to 1, shown as a bar under the thumbnail; `null` for a video that was not watched.
 */
@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    /** An optional action next to the title, for example removing the video from a playlist. */
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(bottom = CardBottomPadding)) {
        Box {
            VideoThumbnail(url = video.thumbnailUrl, shape = RectangleShape, modifier = Modifier.fillMaxWidth())
            if (video.contentKind == VideoContentKind.LIVE_OR_PREMIERE) {
                LiveBadge(modifier = Modifier.align(Alignment.TopStart).padding(BadgeMargin))
            } else {
                video.duration?.let { duration ->
                    DurationBadge(
                        text = formatDuration(duration),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(BadgeMargin),
                    )
                }
                progress?.takeIf { it > 0f }?.let {
                    WatchedProgressBar(progress = it, modifier = Modifier.align(Alignment.BottomStart))
                }
            }
        }
        Row(
            modifier = Modifier.padding(start = TextPadding, top = TextTopPadding, end = TextPadding),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TextSpacing)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.core_ui_video_subtitle, video.channelTitle, relativeDate(video)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            action?.invoke()
        }
    }
}

/** A 16:9 video thumbnail; [url] is `null` when there is nothing to show. */
@Composable
fun VideoThumbnail(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(ThumbnailCornerRadius),
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** "2 hours ago" and the like, relative to now. */
fun relativeDate(video: Video): String =
    DateUtils
        .getRelativeTimeSpanString(
            video.publishedAt.toEpochMilliseconds(),
            Clock.System.now().toEpochMilliseconds(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()

@Composable
private fun WatchedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ProgressBarHeight)
                .progressSemantics(progress)
                .background(Color.White.copy(alpha = PROGRESS_TRACK_ALPHA)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(progress)
                    .height(ProgressBarHeight)
                    .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier =
            modifier
                .background(Color.Black.copy(alpha = BADGE_BACKGROUND_ALPHA), MaterialTheme.shapes.extraSmall)
                .padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
    )
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            modifier
                .background(colors.primary, CircleShape)
                .padding(horizontal = BadgeHorizontalPadding + BadgeVerticalPadding, vertical = BadgeVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(LiveDotSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(LiveDotSize).background(colors.onPrimary, CircleShape))
        Text(
            text = stringResource(R.string.core_ui_video_live),
            color = colors.onPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
