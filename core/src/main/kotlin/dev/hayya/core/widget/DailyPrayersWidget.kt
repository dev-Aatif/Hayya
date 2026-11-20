package dev.hayya.core.widget

import android.content.Context
import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
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
import dev.hayya.core.data.prayerlog.PrayerLogRepository
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS
import dev.hayya.core.system.receivers.PreferenceDataStoreProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ActionCallback triggered when a prayer check button is tapped directly on a widget.
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
 * Interactive 4x2 / 3x2 Daily Prayers & Habit Tracker Widget.
 * Tap any prayer circle to check it off right from your home screen.
 */
class DailyPrayersWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PrayerWidgetUpdater.loadCurrentSnapshot(context)
        val palette = WidgetTheme.paletteFor(snapshot.themeMode)

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(palette.border)
                    .padding(1.dp)
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(23.dp)
                        .background(palette.surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header Row (tapping opens Prayer Habits page)
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(
                                    actionStartActivity(
                                        WidgetTheme.launchIntent(context, PrayerWidgetUpdater.DEST_TRACKER)
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Prayers",
                                style = TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Box(
                                modifier = GlanceModifier
                                    .cornerRadius(10.dp)
                                    .background(palette.activeRowBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${snapshot.completedCount}/5 prayed",
                                    style = TextStyle(
                                        color = palette.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // 5 Interactive Prayer Columns
                        val compactMap = snapshot.compactDailyTimes.toMap()
                        val fullMap = snapshot.dailyTimes.toMap()

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OBLIGATORY_PRAYERS.forEach { prayer ->
                                val isDone = snapshot.completedPrayers.contains(prayer)
                                val isNext = snapshot.nextPrayerType == prayer
                                val timeStr = compactMap[prayer] ?: fullMap[prayer] ?: "--:--"
                                val initial = WidgetTheme.compactInitial(prayer, snapshot.isFriday)
                                val label = WidgetTheme.shortPrayerLabel(prayer, snapshot.isFriday)

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
                                        .padding(vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = GlanceModifier
                                            .size(34.dp)
                                            .cornerRadius(17.dp)
                                            .background(
                                                when {
                                                    isDone -> palette.accent
                                                    isNext -> palette.activeRowBg
                                                    else -> palette.surfaceElevated
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isDone) "✓" else initial,
                                            style = TextStyle(
                                                color = when {
                                                    isDone -> palette.onAccent
                                                    isNext -> palette.accent
                                                    else -> palette.textSecondary
                                                },
                                                fontSize = if (isDone) 15.sp else 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(modifier = GlanceModifier.height(5.dp))

                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = if (isDone || isNext) palette.accent else palette.textPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = if (isNext || isDone) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )

                                    Text(
                                        text = timeStr,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = palette.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = (snapshot.completedCount / 5f).coerceIn(0f, 1f),
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .cornerRadius(2.dp),
                            color = palette.accent,
                            backgroundColor = palette.trackBg
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Footer
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(WidgetTheme.launchIntent(context))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Next: ${snapshot.nextPrayerName} • ${snapshot.nextPrayerTimeFormatted}",
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )

                            Text(
                                text = snapshot.locationName,
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textMuted,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
