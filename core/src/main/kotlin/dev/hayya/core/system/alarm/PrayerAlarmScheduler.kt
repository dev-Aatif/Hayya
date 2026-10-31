package dev.hayya.core.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.batoulapps.adhan.Coordinates
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ScheduledAlert
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.system.receivers.PrayerAlarmReceiver
import dev.hayya.core.system.ui.PrayerWakeActivity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Low-level Android alarm dispatcher responsible for scheduling exact prayer alarms.
 *
 * Exclusively uses [AlarmManager.setAlarmClock] instead of [AlarmManager.setExactAndAllowWhileIdle].
 * In Android's power management hierarchy, [AlarmManager.setAlarmClock]:
 * 1. Has the highest system priority and is exempt from standard Doze mode throttling.
 * 2. Displays the native alarm clock icon in the Android status bar and lockscreen.
 * 3. Guarantees exact sub-second wakefulness even when the device is in deep low-power sleep.
 */
class PrayerAlarmScheduler(
    private val context: Context,
    private val calculationEngine: PrayerCalculationEngine,
    private val preferencesRepository: UserPreferencesRepository? = null,
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
) {

    /**
     * Checks if the app is authorized to schedule exact alarms on Android 12+ (API 31+).
     * On Android 13+ with USE_EXACT_ALARM, this normally evaluates to true automatically.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules a rolling queue of exact alarms for the next [daysAhead] consecutive days (default 7).
     *
     * @param preferences Optional explicit preferences; if null, fetches latest from [preferencesRepository].
     * @param startDate Starting local date; defaults to today in current system timezone.
     * @param currentEpochMillis Current timestamp to filter out expired triggers.
     * @return List of all [ScheduledAlert] instances successfully registered with [AlarmManager].
     */
    suspend fun scheduleRollingAlarms(
        preferences: UserPreferences? = null,
        startDate: LocalDate? = null,
        daysAhead: Int = 7,
        currentEpochMillis: Long = Clock.System.now().toEpochMilliseconds()
    ): List<ScheduledAlert> {
        val userPrefs = preferences ?: preferencesRepository?.userPreferencesFlow?.first()
            ?: UserPreferences()

        val timeZone = TimeZone.currentSystemDefault()
        val effectiveStartDate = startDate ?: Clock.System.now().toLocalDateTime(timeZone).date

        val coordinates = Coordinates(
            userPrefs.location.latitude,
            userPrefs.location.longitude
        )

        val parameters = calculationEngine.createParameters(
            method = userPrefs.calculationMethod.toAdhanMethod(),
            madhab = userPrefs.madhab.toAdhanMadhab(),
            highLatitudeRule = userPrefs.highLatitudeRule.toAdhanRule()
        )

        val alertsToSchedule = calculationEngine.computeRollingQueue(
            startDate = effectiveStartDate,
            coordinates = coordinates,
            parameters = parameters,
            manualOffsets = userPrefs.manualMinuteOffsets,
            ishaExpiresAtMidnight = userPrefs.ishaExpiresAtMidnight,
            daysAhead = daysAhead,
            currentEpochMillis = currentEpochMillis
        )

        val filteredAlerts = alertsToSchedule.filter { alert ->
            if (alert.tier == AlertTier.PRE_REMINDER && !userPrefs.preReminderEnabled) return@filter false
            if (alert.tier == AlertTier.LAST_CALL && !userPrefs.lastCallEnabled) return@filter false
            true
        }

        for (alert in filteredAlerts) {
            scheduleSingleAlert(alert)
        }

        Log.i(
            TAG,
            "Successfully scheduled ${filteredAlerts.size} rolling prayer alarms starting $effectiveStartDate"
        )

        return filteredAlerts
    }

    /**
     * Dispatches an individual alarm via [AlarmManager.setAlarmClock].
     *
     * Request code is deterministically computed as:
     * `(DayOfYear * 100 + PrayerIndex * 10 + TierIndex)`
     */
    fun scheduleSingleAlert(alert: ScheduledAlert) {
        val requestCode = alert.notificationId

        val operationIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_PRAYER_ALARM
            putExtra(EXTRA_NOTIFICATION_ID, alert.notificationId)
            putExtra(EXTRA_PRAYER_TYPE, alert.prayerType.name)
            putExtra(EXTRA_ALERT_TIER, alert.tier.name)
            putExtra(EXTRA_TRIGGER_MILLIS, alert.triggerEpochMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, PrayerWakeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, alert.notificationId)
            putExtra(EXTRA_PRAYER_TYPE, alert.prayerType.name)
            putExtra(EXTRA_ALERT_TIER, alert.tier.name)
        }

        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(alert.triggerEpochMillis, showPendingIntent)

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm clock for notificationId $requestCode", se)
        }
    }

    /**
     * Schedules a simulated test alarm to trigger [delaySeconds] from now.
     * Allows the user to test the real background alarm, WakeLock, AudioService, and Lockscreen Wake screen.
     */
    fun scheduleTestAlarm(delaySeconds: Int, prayerType: PrayerType = PrayerType.ASR) {
        val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        val testNotificationId = 999999

        val operationIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_PRAYER_ALARM
            putExtra(EXTRA_NOTIFICATION_ID, testNotificationId)
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_ALERT_TIER, AlertTier.MAIN_CALL.name)
            putExtra(EXTRA_TRIGGER_MILLIS, triggerMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            testNotificationId,
            operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, PrayerWakeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, testNotificationId)
            putExtra(EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(EXTRA_ALERT_TIER, AlertTier.MAIN_CALL.name)
        }

        val showPendingIntent = PendingIntent.getActivity(
            context,
            testNotificationId,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.i(TAG, "Scheduled test alarm in $delaySeconds seconds ($triggerMillis)")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException scheduling test alarm", se)
        }
    }

    /**
     * Cancels an existing scheduled alarm by its [ScheduledAlert].
     */
    fun cancelAlert(alert: ScheduledAlert) {
        cancelAlertById(alert.notificationId)
    }

    /**
     * Cancels an existing scheduled alarm by its deterministic notification ID.
     */
    fun cancelAlertById(notificationId: Int) {
        val operationIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_PRAYER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            operationIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Cancels all rolling alarms for the next [daysAhead] consecutive days starting from [startDate].
     */
    fun cancelAllScheduledAlarms(
        startDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        daysAhead: Int = 7
    ) {
        for (dayIndex in 0 until daysAhead) {
            val date = startDate.plus(dayIndex, DateTimeUnit.DAY)
            val dayOfYear = date.dayOfYear
            for (prayer in PrayerType.entries) {
                for (tier in AlertTier.entries) {
                    val id = ScheduledAlert.createNotificationId(dayOfYear, prayer, tier)
                    cancelAlertById(id)
                }
            }
        }
        Log.i(TAG, "Cancelled all rolling alarms for $daysAhead days starting from $startDate")
    }

    companion object {
        private const val TAG = "PrayerAlarmScheduler"

        const val ACTION_TRIGGER_PRAYER_ALARM = "dev.hayya.alarm.ACTION_TRIGGER_PRAYER_ALARM"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_ALERT_TIER = "extra_alert_tier"
        const val EXTRA_TRIGGER_MILLIS = "extra_trigger_millis"
    }
}
