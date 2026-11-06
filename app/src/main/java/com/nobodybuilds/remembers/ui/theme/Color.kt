package com.nobodybuilds.remembers.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Nobody Builds "Daylight" palette and tokens (Nobody-Builds-Design/tokens/tokens.json).
val NightShift = Color(0xFF172033)
val Blueprint = Color(0xFF2457A6)
val HardHat = Color(0xFFFDC521)
val Sky = Color(0xFFCFE0F7)
val Butter = Color(0xFFFFE8A3)
val Brick = Color(0xFFEA724A)
val Paper = Color(0xFFF5F7FB)

val LightColors = lightColorScheme(
    primary = Blueprint,
    onPrimary = Paper,
    primaryContainer = Color(0xFFE3ECF8),
    onPrimaryContainer = Blueprint,
    inversePrimary = Color(0xFF7FA8E8),
    secondary = Color(0xFF4F5A70),
    onSecondary = Paper,
    secondaryContainer = Sky,
    onSecondaryContainer = NightShift,
    tertiary = HardHat,
    onTertiary = NightShift,
    tertiaryContainer = Color(0xFFFFF6D6),
    onTertiaryContainer = NightShift,
    error = Color(0xFFB91C1C),
    onError = Paper,
    errorContainer = Color(0xFFFCEBE4),
    onErrorContainer = Color(0xFF851E1D),
    background = Paper,
    onBackground = NightShift,
    surface = Paper,
    onSurface = NightShift,
    surfaceVariant = Color(0xFFECF0F7),
    onSurfaceVariant = Color(0xFF4F5A70),
    surfaceTint = Blueprint,
    inverseSurface = NightShift,
    inverseOnSurface = Paper,
    outline = Color(0xFF7D8699),
    outlineVariant = Color(0xFFD3DAE6),
    scrim = NightShift,
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE4E9F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFECF0F7),
    surfaceContainerHigh = Color(0xFFE4E9F2),
    surfaceContainerHighest = Color(0xFFDDE3EE),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA8E8),
    onPrimary = NightShift,
    primaryContainer = Color(0xFF263A5C),
    onPrimaryContainer = Color(0xFFA9C8F0),
    inversePrimary = Blueprint,
    secondary = Color(0xFFC3C8D2),
    onSecondary = NightShift,
    secondaryContainer = Color(0xFF24344F),
    onSecondaryContainer = Sky,
    tertiary = HardHat,
    onTertiary = NightShift,
    tertiaryContainer = Color(0xFF3A3424),
    onTertiaryContainer = Butter,
    error = Color(0xFFFCA5A5),
    onError = NightShift,
    errorContainer = Color(0xFF3B2C2B),
    onErrorContainer = Color(0xFFF2A88E),
    background = NightShift,
    onBackground = Paper,
    surface = NightShift,
    onSurface = Paper,
    surfaceVariant = Color(0xFF1D2840),
    onSurfaceVariant = Color(0xFFC3C8D2),
    surfaceTint = Color(0xFF7FA8E8),
    inverseSurface = Paper,
    inverseOnSurface = NightShift,
    outline = Color(0xFF8592AD),
    outlineVariant = Color(0xFF3E4D6B),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A3957),
    surfaceDim = Color(0xFF121A2B),
    surfaceContainerLowest = Color(0xFF121A2B),
    surfaceContainerLow = Color(0xFF1D2840),
    surfaceContainer = Color(0xFF1D2840),
    surfaceContainerHigh = Color(0xFF22304B),
    surfaceContainerHighest = Color(0xFF2A3957),
)

/** Status colours from the kit: every status is a word on a soft fill, never colour alone. */
@Immutable
data class StatusColors(
    val successSoft: Color,
    val successInk: Color,
    val warnSoft: Color,
    val warnInk: Color,
    val infoSoft: Color,
    val infoInk: Color,
    val panel: Color,
    val line: Color,
)

val LightStatus = StatusColors(
    successSoft = Color(0xFFF1F9EC), successInk = Color(0xFF3B6B2B),
    warnSoft = Color(0xFFFCEBE4), warnInk = Color(0xFFA3412A),
    infoSoft = Color(0xFFEEF4FC), infoInk = Blueprint,
    panel = Color(0xFFFFFFFF), line = Color(0xFFD3DAE6),
)

val DarkStatus = StatusColors(
    successSoft = Color(0xFF26382F), successInk = Color(0xFFA6D98F),
    warnSoft = Color(0xFF3B2C2B), warnInk = Color(0xFFF2A88E),
    infoSoft = Color(0xFF24344F), infoInk = Sky,
    panel = Color(0xFF22304B), line = Color(0xFF3E4D6B),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }
