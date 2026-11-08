package dev.hayya.core.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import dev.hayya.core.ui.MainActivity

/**
 * Wide 4x2 dual-pane AppWidget showing countdown hero and full daily prayer timetable.
 */
class FullTimetableWidget : GlanceAppWidget() {

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
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(23.dp)
                        .background(ColorProvider(Color(0xFF121417)))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column (Hero ~40% width)
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NEXT PRAYER",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF767E8B)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            Text(
                                text = snapshot.nextPrayerName,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE5C378)),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                text = snapshot.nextPrayerTimeFormatted,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFFFFFFF)),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.height(4.dp))

                            Text(
                                text = snapshot.countdownFormatted,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFE5C378)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            Text(
                                text = snapshot.locationName,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF525A66)),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        // Divider line
                        Box(
                            modifier = GlanceModifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(ColorProvider(Color(0xFF1F2328)))
                        ) {}

                        Spacer(modifier = GlanceModifier.width(12.dp))

                        // Right Column (Timetable ~60% width): 5 rows (Fajr, Dhuhr, Asr, Maghrib, Isha)
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            snapshot.dailyTimes.forEach { (prayer, timeString) ->
                                val prayerDisplayName = prayer.displayName(snapshot.isFriday)
                                val isPast = snapshot.pastPrayers.contains(prayer)
                                val isUpcoming = snapshot.nextPrayerName.equals(prayerDisplayName, ignoreCase = true)

                                val textColor = when {
                                    isUpcoming -> Color(0xFFE5C378)
                                    isPast -> Color(0xFF525A66) // 40% dimmed
                                    else -> Color(0xFFFFFFFF)
                                }

                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .cornerRadius(4.dp)
                                        .background(
                                            if (isUpcoming) ColorProvider(Color(0x22E5C378))
                                            else ColorProvider(Color.Transparent)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = prayerDisplayName,
                                            style = TextStyle(
                                                color = ColorProvider(textColor),
                                                fontSize = 11.sp,
                                                fontWeight = if (isUpcoming) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            modifier = GlanceModifier.defaultWeight()
                                        )

                                        Text(
                                            text = timeString,
                                            style = TextStyle(
                                                color = ColorProvider(textColor),
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
