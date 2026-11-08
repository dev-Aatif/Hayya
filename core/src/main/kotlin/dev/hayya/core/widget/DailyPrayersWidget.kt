package dev.hayya.core.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.hayya.core.data.prayerlog.PrayerLogRepository
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS
import dev.hayya.core.system.receivers.PreferenceDataStoreProvider
import dev.hayya.core.ui.MainActivity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ActionCallback triggered when a prayer indicator button is clicked directly on the widget.
 * Toggles prayer completion state in [PrayerLogRepository] and updates all widgets.
 */
class TogglePrayerActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val prayerName = parameters[PrayerTypeKey] ?: return
            val prayer = runCatching { PrayerType.valueOf(prayerName) }.getOrNull() ?: return
            val dataStore = PreferenceDataStoreProvider.getDataStore(context)
            val repo = PrayerLogRepository(dataStore)
            val timeZone = TimeZone.currentSystemDefault()
            val today = Clock.System.now().toLocalDateTime(timeZone).date
            repo.togglePrayer(today, prayer)

            // Instantly update the clicked widget instance followed by all other widgets
            DailyPrayersWidget().update(context, glanceId)
            PrayerWidgetUpdater.updateAllWidgets(context)
        } catch (e: Exception) {
            Log.e("TogglePrayerAction", "Failed to toggle prayer completion from widget", e)
        }
    }

    companion object {
        val PrayerTypeKey = ActionParameters.Key<String>("prayer_type")
    }
}

/**
 * Interactive 3x2 / 4x2 Home Screen AppWidget dedicated to tracking daily prayer completion and scheduled times.
 * Supports direct in-widget interactive logging where tapping any prayer pip immediately toggles its completion.
 */
class DailyPrayersWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PrayerWidgetUpdater.loadCurrentSnapshot(context)

        provideContent {
            // Container with 1px border (#1F2328) and 24dp corner radii
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(ColorProvider(Color(0xFF1F2328)))
                    .padding(1.dp)
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(23.dp)
                        .background(ColorProvider(Color(0xFF121417)))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header Row: Title & Completion Count (Tapping opens MainActivity)
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAILY PRAYERS",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF767E8B)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Text(
                                text = "${snapshot.completedCount} OF 5 COMPLETED",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE5C378)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // 5 Interactive Prayer Indicator Buttons Row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeMap = snapshot.dailyTimes.toMap()

                            OBLIGATORY_PRAYERS.forEach { prayer ->
                                val isDone = snapshot.completedPrayers.contains(prayer)
                                val isNext = snapshot.nextPrayerName == prayer.displayName(snapshot.isFriday)
                                val timeStr = timeMap[prayer] ?: "--:--"

                                val initial = when (prayer) {
                                    PrayerType.FAJR -> "F"
                                    PrayerType.DHUHR -> if (snapshot.isFriday) "J" else "D"
                                    PrayerType.ASR -> "A"
                                    PrayerType.MAGHRIB -> "M"
                                    PrayerType.ISHA -> "I"
                                    else -> prayer.name.take(1)
                                }

                                Column(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .clickable(
                                            actionRunCallback<TogglePrayerActionCallback>(
                                                actionParametersOf(
                                                    TogglePrayerActionCallback.PrayerTypeKey to prayer.name
                                                )
                                            )
                                        )
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Circular Indicator Pip / Button
                                    Box(
                                        modifier = GlanceModifier
                                            .size(32.dp)
                                            .cornerRadius(16.dp)
                                            .background(
                                                ColorProvider(
                                                    if (isDone) Color(0xFFE5C378)
                                                    else if (isNext) Color(0xFF2B281D)
                                                    else Color(0xFF1B1D21)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isDone) "✓" else initial,
                                            style = TextStyle(
                                                color = ColorProvider(
                                                    if (isDone) Color(0xFF121417)
                                                    else if (isNext) Color(0xFFE5C378)
                                                    else Color(0xFF8B949E)
                                                ),
                                                fontSize = if (isDone) 14.sp else 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(modifier = GlanceModifier.height(4.dp))

                                    // Prayer Label
                                    Text(
                                        text = prayer.name.take(3),
                                        style = TextStyle(
                                            color = ColorProvider(
                                                if (isDone || isNext) Color(0xFFE5C378)
                                                else Color(0xFFEDEDED)
                                            ),
                                            fontSize = 9.sp,
                                            fontWeight = if (isNext || isDone) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )

                                    // Time
                                    Text(
                                        text = timeStr,
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF8B949E)),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Footer: Next Prayer Countdown & Location (Tapping opens MainActivity)
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Next: ${snapshot.nextPrayerName} ${snapshot.countdownFormatted}",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE5C378)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Text(
                                text = snapshot.locationName.uppercase(),
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF484F58)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
