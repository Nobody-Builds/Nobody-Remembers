package com.nobodybuilds.remembers.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nobodybuilds.remembers.R

val Gilroy = FontFamily(
    Font(R.font.gilroy_regular, FontWeight.Normal),
    Font(R.font.gilroy_medium, FontWeight.Medium),
    Font(R.font.gilroy_semibold, FontWeight.SemiBold),
    Font(R.font.gilroy_bold, FontWeight.Bold),
)

/** For one short accent phrase per screen, never more. */
val Unbounded = FontFamily(
    Font(R.font.unbounded, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

private fun heading(size: Int, line: Int) = TextStyle(
    fontFamily = Gilroy, fontWeight = FontWeight.SemiBold, fontSize = size.sp, lineHeight = line.sp, letterSpacing = (-0.025).em,
)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Gilroy, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
)

val AppTypography = Typography(
    displayLarge = heading(57, 64),
    displayMedium = heading(45, 52),
    displaySmall = heading(36, 44),
    headlineLarge = heading(32, 38),
    headlineMedium = heading(28, 34),
    headlineSmall = heading(24, 30),
    titleLarge = heading(22, 28),
    titleMedium = body(16, 24, FontWeight.SemiBold),
    titleSmall = body(14, 20, FontWeight.SemiBold),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 20),
    bodySmall = body(12, 16),
    labelLarge = body(14, 20, FontWeight.SemiBold),
    labelMedium = body(12, 16, FontWeight.SemiBold),
    labelSmall = body(11, 16, FontWeight.Medium),
)

val AccentStyle = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp)
