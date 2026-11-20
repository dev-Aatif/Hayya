package dev.hayya.core.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS

/**
 * Sleek 4x1 horizontal strip widget showing the next prayer and interactive 5-prayer check pips.
 */
class NextPrayerBannerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PrayerWidgetUpdater.loadCurrentSnapshot(context)
        val palette = WidgetTheme.paletteFor(snapshot.themeMode)

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(palette.border)
                    .padding(1.dp)
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(21.dp)
                        .background(palette.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Next prayer & time (opens main dashboard)
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity(WidgetTheme.launchIntent(context))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = snapshot.nextPrayerName,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = palette.accent,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Text(
                                    text = snapshot.nextPrayerTimeFormatted,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = palette.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Text(
                                text = "${snapshot.countdownFormatted} • ${snapshot.locationName}",
                                maxLines = 1,
                                style = TextStyle(
                                    color = palette.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        // Right: 5 interactive compact prayer pips
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OBLIGATORY_PRAYERS.forEachIndexed { index, prayer ->
                                val isDone = snapshot.completedPrayers.contains(prayer)
                                val isNext = snapshot.nextPrayerType == prayer
                                val initial = WidgetTheme.compactInitial(prayer, snapshot.isFriday)

                                if (index > 0) {
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                }

                                Box(
                                    modifier = GlanceModifier
                                        .size(28.dp)
                                        .cornerRadius(14.dp)
                                        .background(
                                            when {
                                                isDone -> palette.accent
                                                isNext -> palette.activeRowBg
                                                else -> palette.surfaceElevated
                                            }
                                        )
                                        .clickable(
                                            actionRunCallback<TogglePrayerActionCallback>(
                                                actionParametersOf(
                                                    TogglePrayerActionCallback.PrayerTypeKey to prayer.name
                                                )
                                            )
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
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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
