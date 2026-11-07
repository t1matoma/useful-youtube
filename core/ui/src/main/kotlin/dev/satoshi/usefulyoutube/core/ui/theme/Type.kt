package dev.satoshi.usefulyoutube.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.satoshi.usefulyoutube.core.ui.R

private val BodyWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val HeadingWeights = listOf(FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** Bundled variable fonts: one file per family, one [Font] per weight that the app uses. */
private fun variableFamily(
    resId: Int,
    weights: List<FontWeight>,
): FontFamily =
    FontFamily(
        weights.map { weight ->
            Font(
                resId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )

/** Inter for text, Onest (a grotesque with Cyrillic) for headings. */
private val BodyFamily = variableFamily(R.font.inter, BodyWeights)
private val HeadingFamily = variableFamily(R.font.onest, HeadingWeights)

private val TightTracking = (-0.02).em
private val SlightTracking = (-0.01).em

private val Default = Typography()

internal val UsefulYouTubeTypography: Typography =
    Typography(
        displayLarge = Default.displayLarge.heading(FontWeight.Bold, TightTracking),
        displayMedium = Default.displayMedium.heading(FontWeight.Bold, TightTracking),
        displaySmall = Default.displaySmall.heading(FontWeight.Bold, TightTracking),
        headlineLarge = Default.headlineLarge.heading(FontWeight.Bold, TightTracking),
        headlineMedium = Default.headlineMedium.heading(FontWeight.Bold, TightTracking),
        headlineSmall = Default.headlineSmall.heading(FontWeight.Bold, TightTracking),
        titleLarge = Default.titleLarge.heading(FontWeight.Bold, SlightTracking).copy(lineHeight = 28.sp),
        titleMedium = Default.titleMedium.body(FontWeight.SemiBold).copy(letterSpacing = 0.sp),
        titleSmall = Default.titleSmall.body(FontWeight.SemiBold),
        bodyLarge = Default.bodyLarge.body(FontWeight.Normal),
        bodyMedium = Default.bodyMedium.body(FontWeight.Normal),
        bodySmall = Default.bodySmall.body(FontWeight.Normal),
        labelLarge = Default.labelLarge.body(FontWeight.SemiBold),
        labelMedium = Default.labelMedium.body(FontWeight.SemiBold),
        labelSmall = Default.labelSmall.body(FontWeight.Medium),
    )

private fun TextStyle.heading(
    weight: FontWeight,
    tracking: TextUnit,
): TextStyle = copy(fontFamily = HeadingFamily, fontWeight = weight, letterSpacing = tracking)

private fun TextStyle.body(weight: FontWeight): TextStyle = copy(fontFamily = BodyFamily, fontWeight = weight)
