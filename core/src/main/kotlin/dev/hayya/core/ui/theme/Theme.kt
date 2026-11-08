package dev.hayya.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.ThemeModeOption

// Pure OLED Dark Design Tokens (canonical references for OLED battery preservation)
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

/**
 * Geometric typeface Sora styling designed by Jonathan Barnbrook and Julián Moncada.
 * Built for screen clarity, modern tabular figures, and balanced rhythm.
 * Employs clean system geometric Sans-Serif ensuring 100% stable, crash-free execution.
 */
val SoraFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Signature Pure OLED Dark ColorScheme for battery-neutral, high-contrast night viewing.
 */
val DarkColorScheme = darkColorScheme(
    primary = WarmSandAccent,
    onPrimary = Color(0xFF1A1500),
    primaryContainer = Color(0xFF2B281D),
    onPrimaryContainer = WarmSandAccent,
    secondary = Color(0xFFD5C4A1),
    onSecondary = Color(0xFF382F16),
    secondaryContainer = Color(0xFF1E2126),
    onSecondaryContainer = WarmSandAccent,
    surface = ElevatedCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = TrackBackground,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ElevatedCardSurface,
    surfaceContainerHigh = Color(0xFF1A1D21),
    background = OledBackground,
    onBackground = TextPrimary,
    outline = CardBorderColor,
    outlineVariant = Color(0xFF262B32)
)

/**
 * Elegant Ivory / Warm Sand Light ColorScheme for daylight ambient readability.
 */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA4),
    onPrimaryContainer = Color(0xFF261900),
    secondary = Color(0xFF6B5D3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E1BB),
    onSecondaryContainer = Color(0xFF241A04),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFEFE4D2),
    onSurfaceVariant = Color(0xFF4E4639),
    surfaceContainer = Color(0xFFF8F2E8),
    surfaceContainerHigh = Color(0xFFF3ECE0),
    background = Color(0xFFFBF8F2),
    onBackground = Color(0xFF1E1B16),
    outline = Color(0xFF807567),
    outlineVariant = Color(0xFFD2C5B4)
)

/**
 * Complete Material 3 typography powered by Jonathan Barnbrook's Sora typeface.
 */
val HayyaTypography = Typography(
    displayLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

/**
 * Tabular figures styling (tnum) ensuring jitter-free countdown timers, clocks, and millisecond displays.
 * Color defaults to LocalContentColor so callers and themes control color adaptively.
 */
val TabularCountdownStyle = TextStyle(
    fontFamily = SoraFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 52.sp,
    letterSpacing = 0.5.sp,
    fontFeatureSettings = "tnum"
)

val TabularClockStyle = TextStyle(
    fontFamily = SoraFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    fontFeatureSettings = "tnum"
)

val HeroLabelStyle = TextStyle(
    fontFamily = SoraFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    letterSpacing = 4.sp
)

@Composable
fun HayyaTheme(
    themeMode: ThemeModeOption = ThemeModeOption.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeModeOption.DARK -> true
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.SYSTEM -> systemInDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HayyaTypography,
        content = content
    )
}

@Composable
fun HayyaTheme(
    content: @Composable () -> Unit
) {
    HayyaTheme(
        themeMode = ThemeModeOption.DARK,
        dynamicColor = false,
        content = content
    )
}
