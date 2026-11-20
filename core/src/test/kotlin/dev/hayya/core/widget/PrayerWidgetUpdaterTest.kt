package dev.hayya.core.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.updateAll
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ThemeModeOption
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrayerWidgetUpdaterTest {

    @Test
    fun actionPrayerDataChanged_hasCorrectActionIdentifier() {
        assertThat(PrayerWidgetUpdater.ACTION_PRAYER_DATA_CHANGED)
            .isEqualTo("dev.hayya.widget.ACTION_PRAYER_DATA_CHANGED")
    }

    @Test
    fun widgetPrayerSnapshot_initializesCorrectDefaults() {
        val snapshot = WidgetPrayerSnapshot(
            nextPrayerName = "ASR",
            nextPrayerTimeFormatted = "03:45 PM",
            countdownFormatted = "in 42m",
            locationName = "Makkah, Saudi Arabia",
            dailyTimes = listOf(
                PrayerType.FAJR to "05:12 AM",
                PrayerType.DHUHR to "12:15 PM",
                PrayerType.ASR to "03:45 PM",
                PrayerType.MAGHRIB to "06:14 PM",
                PrayerType.ISHA to "07:44 PM"
            )
        )

        assertThat(snapshot.nextPrayerName).isEqualTo("ASR")
        assertThat(snapshot.countdownFormatted).isEqualTo("in 42m")
        assertThat(snapshot.dailyTimes).hasSize(5)
        assertThat(snapshot.locationName).contains("Makkah")
        assertThat(snapshot.isFriday).isFalse()
    }

    @Test
    fun widgetPrayerSnapshot_supportsFridayJumuah() {
        val snapshot = WidgetPrayerSnapshot(
            nextPrayerName = "JUMU'AH",
            nextPrayerTimeFormatted = "12:15 PM",
            countdownFormatted = "in 25m",
            isFriday = true
        )

        assertThat(snapshot.isFriday).isTrue()
        assertThat(snapshot.nextPrayerName).isEqualTo("JUMU'AH")
    }

    @Test
    fun compactPrayerWidget_usesExactSizeMode() {
        val widget = CompactPrayerWidget()
        assertThat(widget.sizeMode).isEqualTo(SizeMode.Exact)
    }

    @Test
    fun fullTimetableWidget_usesExactSizeMode() {
        val widget = FullTimetableWidget()
        assertThat(widget.sizeMode).isEqualTo(SizeMode.Exact)
    }

    @Test
    fun dailyPrayersWidget_usesExactSizeMode() {
        val widget = DailyPrayersWidget()
        assertThat(widget.sizeMode).isEqualTo(SizeMode.Exact)
    }

    @Test
    fun nextPrayerBannerWidget_andFastingNightWidget_useExactSizeMode() {
        assertThat(NextPrayerBannerWidget().sizeMode).isEqualTo(SizeMode.Exact)
        assertThat(FastingAndNightWidget().sizeMode).isEqualTo(SizeMode.Exact)
    }

    @Test
    fun widgetReceivers_instantiateGlanceAppWidgetsProperly() {
        val compactReceiver = CompactPrayerWidgetReceiver()
        val timetableReceiver = FullTimetableWidgetReceiver()
        val dailyReceiver = DailyPrayersWidgetReceiver()
        val bannerReceiver = NextPrayerBannerWidgetReceiver()
        val nightReceiver = FastingAndNightWidgetReceiver()

        assertThat(compactReceiver.glanceAppWidget).isInstanceOf(CompactPrayerWidget::class.java)
        assertThat(timetableReceiver.glanceAppWidget).isInstanceOf(FullTimetableWidget::class.java)
        assertThat(dailyReceiver.glanceAppWidget).isInstanceOf(DailyPrayersWidget::class.java)
        assertThat(bannerReceiver.glanceAppWidget).isInstanceOf(NextPrayerBannerWidget::class.java)
        assertThat(nightReceiver.glanceAppWidget).isInstanceOf(FastingAndNightWidget::class.java)
    }

    @Test
    fun widgetTheme_supportsAllThemeModes_andShortLabels() {
        ThemeModeOption.entries.forEach { mode ->
            val palette = WidgetTheme.paletteFor(mode)
            assertThat(palette.accent).isNotNull()
            assertThat(palette.surface).isNotNull()
        }

        assertThat(WidgetTheme.shortPrayerLabel(PrayerType.DHUHR, isFriday = false)).isEqualTo("Dhuhr")
        assertThat(WidgetTheme.shortPrayerLabel(PrayerType.DHUHR, isFriday = true)).isEqualTo("Jumu'ah")
        assertThat(WidgetTheme.compactInitial(PrayerType.DHUHR, isFriday = true)).isEqualTo("J")
    }

    @Test
    fun togglePrayerActionCallback_hasCorrectParameterKey() {
        assertThat(TogglePrayerActionCallback.PrayerTypeKey.name).isEqualTo("prayer_type")
    }

    @Test
    fun widgetPrayerSnapshot_calculatesCompletedCountProperly() {
        val emptySnapshot = WidgetPrayerSnapshot()
        assertThat(emptySnapshot.completedCount).isEqualTo(0)

        val completedSnapshot = WidgetPrayerSnapshot(
            completedPrayers = setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR)
        )
        assertThat(completedSnapshot.completedCount).isEqualTo(3)
    }

    /**
     * Regression: updateAllWidgets used to broadcast ACTION_PRAYER_DATA_CHANGED, which three
     * receivers handle by calling updateAllWidgets again -> infinite, exponentially growing
     * broadcast loop (100% CPU, wake locks, severe battery drain in background).
     */
    @Test
    fun updateAllWidgets_refreshesGlanceWidgets_withoutReBroadcastingDataChanged() = runTest {
        val context = mockk<Context>(relaxed = true)
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        try {
            coEvery { any<GlanceAppWidget>().updateAll(any()) } just Runs

            PrayerWidgetUpdater.updateAllWidgets(context)

            coVerify(exactly = 5) { any<GlanceAppWidget>().updateAll(context) }
            verify(exactly = 0) { context.sendBroadcast(any()) }
            verify(exactly = 0) { context.sendBroadcast(any(), any()) }
        } finally {
            unmockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        }
    }
}
