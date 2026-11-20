package dev.hayya.core.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.hayya.core.domain.model.PrayerType

/**
 * Wide 4x2 dual-pane AppWidget showing the next prayer hero and full 5-prayer timetable with checkmarks.
 */
class FullTimetableWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PrayerWidgetUpdater.loadCurrentSnapshot(context)
        val palette = WidgetTheme.paletteFor(snapshot.themeMode)

        // During Sunrise interval, highlight Dhuhr as the next prayer in the 5-prayer timetable
        val highlightedPrayer = if (snapshot.nextPrayerType == PrayerType.SUNRISE) {
            PrayerType.DHUHR
        } else {
            snapshot.nextPrayerType
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(palette.border)
                    .padding(1.dp)
                    .clickable(actionStartActivity(WidgetTheme.launchIntent(context)))
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(23.dp)
                        .background(palette.surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column: Next Prayer Hero
                        Column(
                            modifier = GlanceModifier
                                .width(124.dp)
                                .fillMaxHeight()
                                .padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = snapshot.locationName,
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Text(
                                text = "Next Prayer",
                                style = TextStyle(
                                    color = palette.textMuted,
                                    fontSize = 10.sp
                                )
                            )

                            Text(
                                text = snapshot.nextPrayerName,
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.accent,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                text = snapshot.nextPrayerTimeFormatted,
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.height(3.dp))

                            Text(
                                text = snapshot.countdownFormatted,
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textSecondary,
                                    fontSize = 11.sp
                                )
                            )

                            Spacer(modifier = GlanceModifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = snapshot.windowProgress.coerceIn(0f, 1f),
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .cornerRadius(2.dp),
                                color = palette.accent,
                                backgroundColor = palette.trackBg
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            if (snapshot.hijriDateShort.isNotEmpty()) {
                                Text(
                                    text = snapshot.hijriDateShort,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = palette.textMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // Divider
                        Box(
                            modifier = GlanceModifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(palette.border)
                        ) {}

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        // Right Column: 5 Daily Prayer Rows
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            snapshot.dailyTimes.forEach { (prayer, timeString) ->
                                val prayerDisplayName = WidgetTheme.shortPrayerLabel(prayer, snapshot.isFriday)
                                val isPast = snapshot.pastPrayers.contains(prayer)
                                val isDone = snapshot.completedPrayers.contains(prayer)
                                val isUpcoming = prayer == highlightedPrayer

                                val textColor = when {
                                    isUpcoming -> palette.accent
                                    isPast && !isDone -> palette.textMuted
                                    else -> palette.textPrimary
                                }

                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.5.dp)
                                        .cornerRadius(6.dp)
                                        .background(
                                            if (isUpcoming) palette.activeRowBg
                                            else ColorProvider(Color.Transparent)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isDone) "✓ $prayerDisplayName" else prayerDisplayName,
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = if (isDone && !isUpcoming) palette.accent else textColor,
                                                fontSize = 11.sp,
                                                fontWeight = if (isUpcoming || isDone) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            modifier = GlanceModifier.defaultWeight()
                                        )

                                        Text(
                                            text = timeString,
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = textColor,
                                                fontSize = 11.sp,
                                                fontWeight = if (isUpcoming) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
