package dev.hayya.core.widget

import androidx.glance.appwidget.SizeMode
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.PrayerType
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
    fun widgetReceivers_instantiateGlanceAppWidgetsProperly() {
        val compactReceiver = CompactPrayerWidgetReceiver()
        val timetableReceiver = FullTimetableWidgetReceiver()

        assertThat(compactReceiver.glanceAppWidget).isInstanceOf(CompactPrayerWidget::class.java)
        assertThat(timetableReceiver.glanceAppWidget).isInstanceOf(FullTimetableWidget::class.java)
    }
}
