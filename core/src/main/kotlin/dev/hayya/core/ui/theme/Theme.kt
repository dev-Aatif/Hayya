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
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ThemeModeOption

// Pure OLED Dark Design Tokens
val OledBackground = Color(0xFF07080A)
val LockscreenWakeBackground = Color(0xFF000000)
val ElevatedCardSurface = Color(0xFF111317)
val CardBorderColor = Color(0xFF1E2229)
val WarmSandAccent = Color(0xFFE7C476)
val WarmSandAccentSubtle = Color(0x33E7C476)

val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

val TrackBackground = Color(0xFF16191F)

// Semantic Status & Celestial Accents
val HayyaSuccess = Color(0xFF34D399)
val HayyaSuccessDark = Color(0xFF15803D)
val HayyaWarning = Color(0xFFFBBF24)

val DawnTint = Color(0xFF818CF8)      // Soft dawn twilight indigo
val SunriseTint = Color(0xFFFBBF24)   // Morning golden amber
val NoonTint = Color(0xFFF59E0B)      // Midday warm gold
val AsrTint = Color(0xFFE7C476)       // Afternoon warm sand
val MaghribTint = Color(0xFFFB7185)   // Sunset coral rose
val IshaTint = Color(0xFF60A5FA)      // Night starlit blue

fun prayerAccentColor(prayerType: PrayerType): Color = when (prayerType) {
    PrayerType.FAJR -> DawnTint
    PrayerType.SUNRISE -> SunriseTint
    PrayerType.DHUHR -> NoonTint
    PrayerType.ASR -> AsrTint
    PrayerType.MAGHRIB -> MaghribTint
    PrayerType.ISHA -> IshaTint
}

val SoraFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Signature Pure OLED Dark ColorScheme for calm, high-contrast viewing.
 */
val DarkColorScheme = darkColorScheme(
    primary = WarmSandAccent,
    onPrimary = Color(0xFF1B1400),
    primaryContainer = Color(0xFF2C2617),
    onPrimaryContainer = WarmSandAccent,
    secondary = Color(0xFFD5C4A1),
    onSecondary = Color(0xFF382F16),
    secondaryContainer = Color(0xFF1B1E24),
    onSecondaryContainer = Color(0xFFEAE0C8),
    tertiary = HayyaSuccess,
    onTertiary = Color(0xFF042F1E),
    surface = ElevatedCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = TrackBackground,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ElevatedCardSurface,
    surfaceContainerHigh = Color(0xFF181B21),
    background = OledBackground,
    onBackground = TextPrimary,
    outline = CardBorderColor,
    outlineVariant = Color(0xFF282D36),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF3B1518)
)

/**
 * Warm Ivory & Sand Light ColorScheme for effortless daytime readability.
 */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8C6200),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE5B4),
    onPrimaryContainer = Color(0xFF2C1D00),
    secondary = Color(0xFF6A5C40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E6CE),
    onSecondaryContainer = Color(0xFF241A06),
    tertiary = HayyaSuccessDark,
    onTertiary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF181612),
    surfaceVariant = Color(0xFFF0E7D8),
    onSurfaceVariant = Color(0xFF574F44),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF7F1E6),
    background = Color(0xFFF8F4EC),
    onBackground = Color(0xFF181612),
    outline = Color(0xFFD8CCB8),
    outlineVariant = Color(0xFFE3D9C9),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2)
)

/**
 * Clean, legible Material 3 typography hierarchy calibrated for effortless reading across all ages.
 */
val HayyaTypography = Typography(
    displayLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = SoraFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

/**
 * Tabular figures styling (tnum) ensuring jitter-free countdown timers and clocks.
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
    fontSize = 16.sp,
    fontFeatureSettings = "tnum"
)

val HeroLabelStyle = TextStyle(
    fontFamily = SoraFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    letterSpacing = 3.sp
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
