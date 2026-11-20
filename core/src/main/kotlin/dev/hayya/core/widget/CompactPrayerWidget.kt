package dev.hayya.core.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Clean 2x2 Home Screen Widget showing the next prayer, time, window progress, and city.
 */
class CompactPrayerWidget : GlanceAppWidget() {

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
                    .clickable(actionStartActivity(WidgetTheme.launchIntent(context)))
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(23.dp)
                        .background(palette.surface)
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Top Row: City + Tap-to-Refresh indicator
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = snapshot.locationName,
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )

                            Box(
                                modifier = GlanceModifier
                                    .cornerRadius(10.dp)
                                    .background(palette.surfaceElevated)
                                    .clickable(actionRunCallback<RefreshWidgetsActionCallback>())
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "↻",
                                    style = TextStyle(
                                        color = palette.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Text(
                            text = "Next Prayer",
                            style = TextStyle(
                                color = palette.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        Text(
                            text = snapshot.nextPrayerName,
                            maxLines = 1,
                            style = TextStyle(
                                color = palette.accent,
                                fontSize = 22.sp,
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

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        Text(
                            text = snapshot.countdownFormatted,
                            maxLines = 1,
                            style = TextStyle(
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        LinearProgressIndicator(
                            progress = snapshot.windowProgress.coerceIn(0f, 1f),
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .cornerRadius(2.dp),
                            color = palette.accent,
                            backgroundColor = palette.trackBg
                        )

                        if (snapshot.hijriDateShort.isNotEmpty()) {
                            Spacer(modifier = GlanceModifier.height(5.dp))
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
                }
            }
        }
    }
}
