package dev.hayya.core.widget

import android.content.Context
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Dedicated 3x2 / 4x2 Night & Fasting AppWidget displaying Suhoor, Iftar, Last Third (Tahajjud), and Midnight.
 */
class FastingAndNightWidget : GlanceAppWidget() {

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
                    .clickable(
                        actionStartActivity(
                            WidgetTheme.launchIntent(context, PrayerWidgetUpdater.DEST_TAHAJJUD)
                        )
                    )
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
                        // Header Row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Night & Fasting",
                                style = TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )

                            if (snapshot.isTahajjudActive) {
                                Box(
                                    modifier = GlanceModifier
                                        .cornerRadius(10.dp)
                                        .background(palette.activeRowBg)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Tahajjud Now",
                                        style = TextStyle(
                                            color = palette.accent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            } else if (snapshot.hijriDateShort.isNotEmpty()) {
                                Text(
                                    text = snapshot.hijriDateShort,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = palette.accent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // 2x2 Grid of Night & Fasting Times
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            NightStatBox(
                                label = "Suhoor Ends",
                                time = snapshot.suhoorFormatted,
                                highlighted = false,
                                palette = palette,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            NightStatBox(
                                label = "Iftar Time",
                                time = snapshot.iftarFormatted,
                                highlighted = false,
                                palette = palette,
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            NightStatBox(
                                label = "Last Third",
                                time = snapshot.tahajjudStartFormatted,
                                highlighted = true,
                                palette = palette,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            NightStatBox(
                                label = "Midnight",
                                time = snapshot.islamicMidnightFormatted,
                                highlighted = false,
                                palette = palette,
                                modifier = GlanceModifier.defaultWeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NightStatBox(
    label: String,
    time: String,
    highlighted: Boolean,
    palette: WidgetPalette,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .cornerRadius(12.dp)
            .background(if (highlighted) palette.activeRowBg else palette.surfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = if (highlighted) palette.accent else palette.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = time,
                maxLines = 1,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
