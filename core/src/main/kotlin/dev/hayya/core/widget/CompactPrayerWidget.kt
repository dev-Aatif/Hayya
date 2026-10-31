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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.hayya.core.ui.MainActivity

/**
 * Ultra-minimalist 2x2 home screen AppWidget displaying upcoming prayer countdown.
 */
class CompactPrayerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PrayerWidgetUpdater.loadCurrentSnapshot(context)

        provideContent {
            // Luxury OLED Dark Container: 1px border (#1F2328) with 24dp rounded corners
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
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Micro-label: NEXT PRAYER in tracked slate (#767E8B)
                        Text(
                            text = "NEXT PRAYER",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF767E8B)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Prayer Name: e.g. "MAGHRIB" in bold white (#FFFFFF)
                        Text(
                            text = snapshot.nextPrayerName,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFFFFFFF)),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // Target Time & Live Countdown in Warm Sand (#E5C378)
                        Text(
                            text = "${snapshot.nextPrayerTimeFormatted} (${snapshot.countdownFormatted})",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFE5C378)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Location context
                        Text(
                            text = snapshot.locationName,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF525A66)),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
