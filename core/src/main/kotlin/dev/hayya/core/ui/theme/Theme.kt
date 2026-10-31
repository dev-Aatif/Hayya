package dev.hayya.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Pure OLED Dark Design Tokens
val OledBackground = Color(0xFF08090A)
val LockscreenWakeBackground = Color(0xFF000000)
val ElevatedCardSurface = Color(0xFF121417)
val CardBorderColor = Color(0xFF1F2328)
val WarmSandAccent = Color(0xFFE5C378)
val WarmSandAccentSubtle = Color(0x33E5C378)

val TextPrimary = Color(0xFFEDEDED)
val TextSecondary = Color(0xFF8B949E)
val TextMuted = Color(0xFF484F58)

val TrackBackground = Color(0xFF141619)

private val DarkColorScheme = darkColorScheme(
    primary = WarmSandAccent,
    onPrimary = Color(0xFF1A1500),
    primaryContainer = ElevatedCardSurface,
    onPrimaryContainer = WarmSandAccent,
    surface = ElevatedCardSurface,
    onSurface = TextPrimary,
    background = OledBackground,
    onBackground = TextPrimary,
    outline = CardBorderColor,
    outlineVariant = CardBorderColor
)

/**
 * Tabular figures styling (tnum) ensuring jitter-free countdown timers, clocks, and millisecond displays.
 */
val TabularCountdownStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 52.sp,
    letterSpacing = 0.5.sp,
    fontFeatureSettings = "tnum",
    color = WarmSandAccent
)

val TabularClockStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    fontFeatureSettings = "tnum",
    color = TextPrimary
)

val HeroLabelStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    letterSpacing = 4.sp,
    color = TextSecondary
)

@Composable
fun HayyaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
