package dev.satoshi.usefulyoutube.core.ui.video

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
private const val BADGE_BACKGROUND_ALPHA = 0.8f
private const val TITLE_MAX_LINES = 2
private val CardPadding = 12.dp
private val BadgePadding = 6.dp
private val BadgeInnerPadding = 4.dp
private val ThumbnailCornerRadius = 12.dp

@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(CardPadding)) {
        Box {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            durationLabel(video)?.let { label ->
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(BadgePadding)
                            .background(
                                Color.Black.copy(alpha = BADGE_BACKGROUND_ALPHA),
                                RoundedCornerShape(BadgeInnerPadding),
                            ).padding(horizontal = BadgeInnerPadding),
                )
            }
        }
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = CardPadding),
        )
        Text(
            text = stringResource(R.string.core_ui_video_subtitle, video.channelTitle, relativeDate(video)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun durationLabel(video: Video): String? =
    when {
        video.contentKind == VideoContentKind.LIVE_OR_PREMIERE -> stringResource(R.string.core_ui_video_live)
        else -> video.duration?.let(::formatDuration)
    }

private fun relativeDate(video: Video): String =
    DateUtils
        .getRelativeTimeSpanString(
            video.publishedAt.toEpochMilliseconds(),
            Clock.System.now().toEpochMilliseconds(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
