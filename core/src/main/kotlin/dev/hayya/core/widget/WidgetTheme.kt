package dev.hayya.core.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.ui.MainActivity

/**
 * Adaptive widget color tokens honoring Hayya's Dark OLED, Warm Sand Light, or System theme.
 */
data class WidgetPalette(
    val border: ColorProvider,
    val surface: ColorProvider,
    val surfaceElevated: ColorProvider,
    val textPrimary: ColorProvider,
    val textSecondary: ColorProvider,
    val textMuted: ColorProvider,
    val accent: ColorProvider,
    val onAccent: ColorProvider,
    val activeRowBg: ColorProvider,
    val trackBg: ColorProvider,
    val success: ColorProvider
)

object WidgetTheme {
    private val DarkBorder = Color(0xFF232730)
    private val DarkSurface = Color(0xFF111317)
    private val DarkSurfaceElevated = Color(0xFF1A1D24)
    private val DarkTextPrimary = Color(0xFFF3F4F6)
    private val DarkTextSecondary = Color(0xFF9CA3AF)
    private val DarkTextMuted = Color(0xFF6B7280)
    private val DarkAccent = Color(0xFFE7C476)
    private val DarkOnAccent = Color(0xFF1B1400)
    private val DarkActiveRowBg = Color(0x28E7C476)
    private val DarkTrackBg = Color(0xFF232730)
    private val DarkSuccess = Color(0xFF34D399)

    private val LightBorder = Color(0xFFE3D9C9)
    private val LightSurface = Color(0xFFFFFCF7)
    private val LightSurfaceElevated = Color(0xFFF0E7D8)
    private val LightTextPrimary = Color(0xFF181612)
    private val LightTextSecondary = Color(0xFF574F44)
    private val LightTextMuted = Color(0xFF8A7F70)
    private val LightAccent = Color(0xFF8C6200)
    private val LightOnAccent = Color(0xFFFFFFFF)
    private val LightActiveRowBg = Color(0x208C6200)
    private val LightTrackBg = Color(0xFFE5DAC8)
    private val LightSuccess = Color(0xFF15803D)

    fun paletteFor(themeMode: ThemeModeOption): WidgetPalette {
        fun pick(light: Color, dark: Color): ColorProvider = when (themeMode) {
            ThemeModeOption.DARK -> ColorProvider(dark)
            ThemeModeOption.LIGHT -> ColorProvider(light)
            ThemeModeOption.SYSTEM -> ColorProvider(day = light, night = dark)
        }

        return WidgetPalette(
            border = pick(LightBorder, DarkBorder),
            surface = pick(LightSurface, DarkSurface),
            surfaceElevated = pick(LightSurfaceElevated, DarkSurfaceElevated),
            textPrimary = pick(LightTextPrimary, DarkTextPrimary),
            textSecondary = pick(LightTextSecondary, DarkTextSecondary),
            textMuted = pick(LightTextMuted, DarkTextMuted),
            accent = pick(LightAccent, DarkAccent),
            onAccent = pick(LightOnAccent, DarkOnAccent),
            activeRowBg = pick(LightActiveRowBg, DarkActiveRowBg),
            trackBg = pick(LightTrackBg, DarkTrackBg),
            success = pick(LightSuccess, DarkSuccess)
        )
    }

    fun launchIntent(context: Context, destination: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (destination != null) {
                putExtra(PrayerWidgetUpdater.EXTRA_START_DESTINATION, destination)
            }
        }
    }

    fun shortPrayerLabel(prayer: PrayerType, isFriday: Boolean): String = when (prayer) {
        PrayerType.FAJR -> "Fajr"
        PrayerType.SUNRISE -> "Sunrise"
        PrayerType.DHUHR -> if (isFriday) "Jumu'ah" else "Dhuhr"
        PrayerType.ASR -> "Asr"
        PrayerType.MAGHRIB -> "Maghrib"
        PrayerType.ISHA -> "Isha"
    }

    fun compactInitial(prayer: PrayerType, isFriday: Boolean): String = when (prayer) {
        PrayerType.FAJR -> "F"
        PrayerType.DHUHR -> if (isFriday) "J" else "D"
        PrayerType.ASR -> "A"
        PrayerType.MAGHRIB -> "M"
        PrayerType.ISHA -> "I"
        PrayerType.SUNRISE -> "S"
    }
}

/**
 * ActionCallback allowing any widget to manually trigger an immediate refresh of all Hayya widgets.
 */
class RefreshWidgetsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            PrayerWidgetUpdater.updateAllWidgets(context)
        } catch (e: Exception) {
            Log.e("RefreshWidgetsAction", "Failed to refresh widgets", e)
        }
    }
}
