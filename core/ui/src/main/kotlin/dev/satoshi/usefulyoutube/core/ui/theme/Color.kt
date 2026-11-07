package dev.satoshi.usefulyoutube.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF0E0E10)
private val InkLowest = Color(0xFF0A0A0C)
private val InkLow = Color(0xFF141417)
private val InkMid = Color(0xFF1A1A1E)
private val InkHigh = Color(0xFF222227)
private val InkHighest = Color(0xFF2A2A30)
private val Paper = Color(0xFFF2F2F3)
private val PaperDim = Color(0xFFC9C9CF)
private val Muted = Color(0xFFA3A3AB)
private val Outline = Color(0xFF4A4A52)
private val OutlineSubtle = Color(0xFF2C2C32)
private val Coral = Color(0xFFFF5A4E)
private val CoralDeep = Color(0xFF3A1916)
private val CoralPale = Color(0xFFFFDAD5)
private val CoralDark = Color(0xFFB3261E)
private val Teal = Color(0xFF7FC8C1)
private val TealDeep = Color(0xFF1B3B38)
private val TealPale = Color(0xFFCDEDE9)
private val ErrorLight = Color(0xFFFFB4AB)
private val OnError = Color(0xFF690005)
private val ErrorDeep = Color(0xFF93000A)
private val ErrorPale = Color(0xFFFFDAD6)

/** The only color scheme of the app: a dark "cinema" palette with a coral accent. */
internal val UsefulYouTubeColorScheme: ColorScheme =
    darkColorScheme(
        primary = Coral,
        onPrimary = Ink,
        primaryContainer = CoralDeep,
        onPrimaryContainer = CoralPale,
        inversePrimary = CoralDark,
        secondary = PaperDim,
        onSecondary = Ink,
        secondaryContainer = InkHighest,
        onSecondaryContainer = Paper,
        tertiary = Teal,
        onTertiary = Ink,
        tertiaryContainer = TealDeep,
        onTertiaryContainer = TealPale,
        background = Ink,
        onBackground = Paper,
        surface = Ink,
        onSurface = Paper,
        surfaceVariant = InkMid,
        onSurfaceVariant = Muted,
        surfaceTint = Muted,
        inverseSurface = Paper,
        inverseOnSurface = InkMid,
        error = ErrorLight,
        onError = OnError,
        errorContainer = ErrorDeep,
        onErrorContainer = ErrorPale,
        outline = Outline,
        outlineVariant = OutlineSubtle,
        scrim = Color.Black,
        surfaceBright = InkHighest,
        surfaceDim = Ink,
        surfaceContainerLowest = InkLowest,
        surfaceContainerLow = InkLow,
        surfaceContainer = InkMid,
        surfaceContainerHigh = InkHigh,
        surfaceContainerHighest = InkHighest,
    )
