package dev.hayya.core.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * GlanceAppWidgetReceiver managing the 2x2 CompactPrayerWidget lifecycle.
 */
class CompactPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactPrayerWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == PrayerWidgetUpdater.ACTION_PRAYER_DATA_CHANGED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                try {
                    PrayerWidgetUpdater.updateAllWidgets(context)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}

/**
 * GlanceAppWidgetReceiver managing the 4x2 FullTimetableWidget lifecycle.
 */
class FullTimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullTimetableWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == PrayerWidgetUpdater.ACTION_PRAYER_DATA_CHANGED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                try {
                    PrayerWidgetUpdater.updateAllWidgets(context)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}

/**
 * GlanceAppWidgetReceiver managing the DailyPrayersWidget lifecycle.
 */
class DailyPrayersWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyPrayersWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == PrayerWidgetUpdater.ACTION_PRAYER_DATA_CHANGED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                try {
                    PrayerWidgetUpdater.updateAllWidgets(context)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}

/**
 * BroadcastReceiver listening for [PrayerWidgetUpdater.ACTION_PRAYER_DATA_CHANGED] to update widgets.
 */
class PrayerWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == PrayerWidgetUpdater.ACTION_PRAYER_DATA_CHANGED) {
            Log.d("PrayerWidgetReceiver", "Received ACTION_PRAYER_DATA_CHANGED, refreshing all widgets")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                try {
                    PrayerWidgetUpdater.updateAllWidgets(context)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}
