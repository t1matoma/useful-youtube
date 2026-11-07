package dev.satoshi.usefulyoutube.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val UsefulYouTubeShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/** The app is always dark and uses its own palette instead of the wallpaper colors. */
@Composable
fun UsefulYouTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UsefulYouTubeColorScheme,
        typography = UsefulYouTubeTypography,
        shapes = UsefulYouTubeShapes,
        content = content,
    )
}
