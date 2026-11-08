package dev.hayya.core.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.friday.FridayReminders
import dev.hayya.core.domain.friday.FridaySalawatReminder
import dev.hayya.core.domain.friday.FridaySurahKahfReminder
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.prayerlog.DailyPrayerLog
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS
import dev.hayya.core.domain.prayerlog.RollingPrayerHistory
import dev.hayya.core.domain.ramadan.IslamicDateInfo
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TabularCountdownStyle
import dev.hayya.core.ui.viewmodel.PrayerDashboardUiState
import dev.hayya.core.ui.viewmodel.PrayerTimeRowUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Modern Minimalist, distraction-free main prayer dashboard.
 * Designed with calm macro-whitespace, a unified timetable surface,
 * tactile daily obligation pips, and a low-profile collapsible Night/Fasting utility.
 */
@Composable
fun MainDashboardScreen(
    state: PrayerDashboardUiState,
    onOpenSettings: () -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenQibla: () -> Unit = {},
    onOpenRamadan: () -> Unit = {},
    onToggleAudioMode: (PrayerType) -> Unit,
    onTogglePrayerCompletion: (PrayerType) -> Unit = {},
    onToggleNightFastingCard: () -> Unit = {},
    onToggleTahajjudAlarm: (Boolean) -> Unit = {},
    onToggleSurahKahfRead: () -> Unit = {},
    onActivateKhutbahMute: () -> Unit = {},
    onRestoreRingerMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Streamlined Header: Location, Hijri Calendar & Seasonal Badge
            DashboardTopBar(
                locationName = state.locationName,
                hijriDateString = state.hijriDateString,
                islamicDateInfo = state.islamicDateInfo,
                isFriday = state.isFriday,
                onOpenRamadan = onOpenRamadan
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Section: Next Prayer Tabular Countdown & Hairline Progress
            HeroCountdownSection(
                nextPrayerName = state.nextPrayerName,
                countdown = state.liveCountdownString,
                timeFormatted = state.nextPrayerTimeFormatted,
                progressRatio = state.progressRatio
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Habit Tracker: 5 Minimalist Circular Indicator Pips & 7-Day Trendline
            PrayerTrackerCard(
                dailyLog = state.todayPrayerLog,
                history = state.rollingPrayerHistory,
                isFriday = state.isFriday,
                onTogglePrayer = onTogglePrayerCompletion
            )

            // Friday (Jumu'ah) Mode: Spiritual Reminders & Khutbah Silence Card
            if (state.isFriday) {
                Spacer(modifier = Modifier.height(16.dp))

                FridayRemindersCard(
                    isSurahKahfRead = state.isSurahKahfRead,
                    isAutoMuted = state.savedRingerState.isAutoMuted,
                    autoMuteEndEpochMillis = state.savedRingerState.endEpochMillis,
                    fridayMinutes = state.userPreferences.fridayAutoMuteDuration.resolveMinutes(state.userPreferences.autoMuteDuration).let {
                        if (it > 0) it else 45
                    },
                    onToggleSurahKahfRead = onToggleSurahKahfRead,
                    onActivateKhutbahMute = onActivateKhutbahMute,
                    onRestoreRingerMode = onRestoreRingerMode
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timetable Section: Unified Daily Schedule in a single continuous container
            TimetableSection(
                prayers = state.dailyPrayers,
                onToggleAudioMode = onToggleAudioMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Supplementary Utility: Collapsible Night & Voluntary Fasting Card
            NightFastingCard(
                state = state,
                isExpanded = state.isNightFastingCardExpanded,
                onToggleExpanded = onToggleNightFastingCard,
                onToggleTahajjudAlarm = onToggleTahajjudAlarm
            )

            // Bottom clearance ensuring the floating navbar never obstructs content
            Spacer(modifier = Modifier.height(112.dp))
        }
    }
}

@Composable
private fun DashboardTopBar(
    locationName: String,
    hijriDateString: String,
    islamicDateInfo: IslamicDateInfo,
    isFriday: Boolean = false,
    onOpenRamadan: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Text(
                    text = locationName.uppercase(),
                    style = TabularClockStyle.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (hijriDateString.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hijriDateString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Active spiritual season indicator badge (Ramadan, Eid) or Friday Jumu'ah pill
        if (islamicDateInfo.isRamadan || islamicDateInfo.isEidAlFitr || islamicDateInfo.isEidAlAdha) {
            val badgeText = when {
                islamicDateInfo.isRamadan -> "RAMADAN • DAY ${islamicDateInfo.hijriDay}"
                islamicDateInfo.isEidAlFitr -> "EID AL-FITR"
                islamicDateInfo.isEidAlAdha -> "EID AL-ADHA"
                else -> "SEASON"
            }
            Surface(
                shape = RoundedCornerShape(9999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .clickable(onClick = onOpenRamadan)
            ) {
                Text(
                    text = badgeText,
                    style = TabularClockStyle.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        } else if (isFriday) {
            Surface(
                shape = RoundedCornerShape(9999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "JUMU'AH • الجمعة",
                    style = TabularClockStyle.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroCountdownSection(
    nextPrayerName: String,
    countdown: String,
    timeFormatted: String,
    progressRatio: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio.coerceIn(0.0f, 1.0f),
        animationSpec = tween(durationMillis = 800),
        label = "WindowProgress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(9999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Text(
                text = "NEXT PRAYER • $nextPrayerName",
                style = TabularClockStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = countdown,
            style = TabularCountdownStyle.copy(
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Begins at $timeFormatted",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Understated hairline progress indicator
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .width(140.dp)
                .height(2.5.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Unified Daily Prayer Schedule inside a single continuous container.
 * Eliminates visual clutter by replacing individual separate card boxes
 * with cohesive rows, soft active prayer highlighting, and dimmed past prayers.
 */
@Composable
private fun TimetableSection(
    prayers: List<PrayerTimeRowUiState>,
    onToggleAudioMode: (PrayerType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Schedule Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S SCHEDULE",
                    style = TabularClockStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Text(
                    text = "ALERT MODE",
                    style = TabularClockStyle.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            )

            // The Prayer Rows inside the single unified timetable
            prayers.forEachIndexed { index, prayer ->
                PrayerRowItem(
                    prayer = prayer,
                    onToggleAudioMode = { onToggleAudioMode(prayer.prayerType) }
                )

                if (index < prayers.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f))
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerRowItem(
    prayer: PrayerTimeRowUiState,
    onToggleAudioMode: () -> Unit
) {
    val isElevated = prayer.isActive
    val rowBackground = if (isElevated) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    val contentAlpha = if (prayer.isPast && !prayer.isActive) 0.45f else 1.0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(contentAlpha),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (prayer.isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (!prayer.isPast) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            else Color.Transparent,
                            CircleShape
                        )
                )
            }

            Text(
                text = prayer.name,
                style = TabularClockStyle.copy(
                    fontSize = 14.sp,
                    fontWeight = if (prayer.isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (prayer.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = prayer.timeFormatted,
                style = TabularClockStyle.copy(
                    fontSize = 14.sp,
                    fontWeight = if (prayer.isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (prayer.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )

            // Direct inline audio mode selector
            AudioModeIcon(
                mode = prayer.audioMode,
                onClick = onToggleAudioMode
            )
        }
    }
}

@Composable
private fun AudioModeIcon(
    mode: AlertMode,
    onClick: () -> Unit
) {
    val (icon, tint) = when (mode) {
        AlertMode.FULL_ALARM -> Icons.Outlined.NotificationsActive to MaterialTheme.colorScheme.primary
        AlertMode.GENTLE_BANNER -> Icons.Outlined.Notifications to MaterialTheme.colorScheme.secondary
        AlertMode.MUTED -> Icons.Outlined.NotificationsOff to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = mode.displayName,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

/**
 * Modern Minimalist daily prayer log card featuring five circular indicator pips and a compact 7-day micro trendline.
 */
@Composable
fun PrayerTrackerCard(
    dailyLog: DailyPrayerLog,
    history: RollingPrayerHistory,
    isFriday: Boolean = false,
    onTogglePrayer: (PrayerType) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title & Completion Ratio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY OBLIGATIONS",
                    style = TabularClockStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Text(
                    text = "${dailyLog.completedCount} OF ${dailyLog.totalCount} COMPLETED",
                    style = TabularClockStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = if (dailyLog.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Five Minimalist Circular Indicator Pips (Fajr, Dhuhr, Asr, Maghrib, Isha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OBLIGATORY_PRAYERS.forEach { prayer ->
                    val isDone = dailyLog.isPrayerCompleted(prayer)
                    val initial = when (prayer) {
                        PrayerType.FAJR -> "F"
                        PrayerType.DHUHR -> if (isFriday) "J" else "D"
                        PrayerType.ASR -> "A"
                        PrayerType.MAGHRIB -> "M"
                        PrayerType.ISHA -> "I"
                        else -> prayer.name.take(1)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onTogglePrayer(prayer)
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDone) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDone) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "${prayer.name} completed",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = initial,
                                    style = TabularClockStyle.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = prayer.name,
                            style = TabularClockStyle.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp,
                                color = if (isDone) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }

            // Compact 7-Day Micro Trendline (Clean & low profile)
            if (history.dailyLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pct = (history.overallRatio * 100).roundToInt()
                    Text(
                        text = "7-DAY RECORD",
                        style = TabularClockStyle.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )

                    // 7 micro pills representing the week
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val logs = history.dailyLogs.takeLast(7)
                        logs.forEach { log ->
                            val fraction = log.completionRatio
                            val barColor = if (fraction >= 1.0f) {
                                MaterialTheme.colorScheme.primary
                            } else if (fraction > 0f) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    Text(
                        text = "$pct%",
                        style = TabularClockStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pct >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

/**
 * Elegant, minimalist Friday (Yawm al-Jumu'ah) card featuring Surah Al-Kahf badge,
 * sending blessings upon Prophet Muhammad ﷺ (Salawat), and one-tap Khutbah silence action.
 */
@Composable
fun FridayRemindersCard(
    isSurahKahfRead: Boolean,
    isAutoMuted: Boolean,
    autoMuteEndEpochMillis: Long,
    fridayMinutes: Int,
    onToggleSurahKahfRead: () -> Unit,
    onActivateKhutbahMute: () -> Unit,
    onRestoreRingerMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Jumu'ah Title and Surah Al-Kahf Action Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = "FRIDAY SUNNAH REMINDERS",
                            style = TabularClockStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Yawm al-Jumu'ah • يَوْمُ الْجُمُعَةِ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Surah Al-Kahf Interactive Badge
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = if (isSurahKahfRead) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSurahKahfRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(onClick = onToggleSurahKahfRead)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isSurahKahfRead) "✓ AL-KAHF READ" else "AL-KAHF • MARK READ",
                            style = TabularClockStyle.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = if (isSurahKahfRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Surah Al-Kahf Authentic Hadith Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SURAH AL-KAHF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FridaySurahKahfReminder.ARABIC_TITLE,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "“${FridaySurahKahfReminder.TRANSLATION}”",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FridaySurahKahfReminder.REFERENCE,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Blessings Upon Prophet Muhammad ﷺ (Salawat) Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DUROOD & SALAWAT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FridaySalawatReminder.ARABIC_TITLE,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FridaySalawatReminder.ARABIC_DUROOD,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${FridaySalawatReminder.HADITH_QUOTATION}”",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FridaySalawatReminder.REFERENCE,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Khutbah Auto-Mute Quick Action Button (for early attendees or automatic window)
            if (isAutoMuted) {
                val formattedTime = remember(autoMuteEndEpochMillis) {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(autoMuteEndEpochMillis))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KHUTBAH AUTO-MUTE ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Silenced until $formattedTime • Restoring normal mode",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onRestoreRingerMode,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Unmute Now",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onActivateKhutbahMute)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SILENCE FOR KHUTBAH (${fridayMinutes}m)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Arriving early? Tap to silence phone for sermon + prayer with auto-restore",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Mute",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Low-profile, modern minimalist collapsible card for Tahajjud (Last Third of the Night),
 * Islamic Midnight, Suhur cutoff (Fajr), and Iftar (Maghrib).
 * Uses flat, unnested layouts to prevent information overload.
 */
@Composable
fun NightFastingCard(
    state: PrayerDashboardUiState,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleTahajjudAlarm: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon, Title, Status Summary, and Expand/Collapse Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TAHAJJUD & FASTING",
                                style = TabularClockStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            if (state.isTahajjudActiveNow) {
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = TabularClockStyle.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        val summaryText = if (state.isTahajjudActiveNow) {
                            "Tahajjud active • Ends at Fajr (${state.suhoorCutoffFormatted})"
                        } else {
                            "Last Third in ${state.tahajjudCountdown} • Starts at ${state.tahajjudStartTimeFormatted}"
                        }

                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    )

                    // 1. Tahajjud Section (Flat layout, no nested cards)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.isTahajjudActiveNow) "TAHAJJUD WINDOW" else "COUNTDOWN TO TAHAJJUD",
                                style = TabularClockStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            Text(
                                text = state.tahajjudCountdown,
                                style = TabularCountdownStyle.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // 2-Column Night Calculation Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "ISLAMIC MIDNIGHT",
                                        style = TabularClockStyle.copy(
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.islamicMidnightFormatted,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Nisf al-Layl",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "LAST THIRD BEGINS",
                                        style = TabularClockStyle.copy(
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.tahajjudStartTimeFormatted,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Thuluth al-Akhir",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }
                        }

                        // Gentle Pre-Alarm Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gentle Tahajjud Pre-Alarm",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Exact alarm trigger at the start of Last Third",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Switch(
                                checked = state.userPreferences.tahajjudAlarmEnabled,
                                onCheckedChange = onToggleTahajjudAlarm,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    )

                    // 2. Fasting Section (Suhur & Iftar)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val fastingBadgeText = if (state.isSunnahFastingToday || state.islamicDateInfo.isRamadan) {
                                state.fastingDayTitle.ifEmpty { "Sunnah Fast Today" }
                            } else if (state.isSunnahFastingTomorrow) {
                                "Tomorrow: ${state.tomorrowFastingDayTitle}"
                            } else {
                                "Sunnah Fasting Guide"
                            }

                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = fastingBadgeText.uppercase(),
                                    style = TabularClockStyle.copy(
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            val countdownLabel = if (state.isFastingCountdownToIftar) "Iftar in" else "Suhur in"
                            Text(
                                text = "$countdownLabel ${state.fastingCountdown}",
                                style = TabularClockStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "SUHUR CUTOFF",
                                        style = TabularClockStyle.copy(
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.suhoorCutoffFormatted,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Fajr (Cease eating)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "IFTAR TIME",
                                        style = TabularClockStyle.copy(
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.iftarFormatted,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Maghrib (Break fast)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

@Preview(name = "Main Dashboard Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Main Dashboard Light", showBackground = true)
@Composable
private fun MainDashboardScreenPreview() {
    HayyaTheme {
        MainDashboardScreen(
            state = PrayerDashboardUiState(
                locationName = "Makkah, Saudi Arabia",
                nextPrayerType = PrayerType.ASR,
                nextPrayerName = "ASR",
                nextPrayerTimeFormatted = "3:45 PM",
                liveCountdownString = "01:42:18",
                progressRatio = 0.65f,
                hijriDateString = "14 Jumada al-Ula 1448 AH",
                dailyPrayers = listOf(
                    PrayerTimeRowUiState(PrayerType.FAJR, "FAJR", 0L, "05:15 AM", isPast = true, isActive = false, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.SUNRISE, "SUNRISE", 0L, "06:30 AM", isPast = true, isActive = false, audioMode = AlertMode.GENTLE_BANNER),
                    PrayerTimeRowUiState(PrayerType.DHUHR, "DHUHR", 0L, "12:15 PM", isPast = true, isActive = false, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.ASR, "ASR", 0L, "03:45 PM", isPast = false, isActive = true, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.MAGHRIB, "MAGHRIB", 0L, "06:20 PM", isPast = false, isActive = false, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.ISHA, "ISHA", 0L, "07:50 PM", isPast = false, isActive = false, audioMode = AlertMode.MUTED)
                )
            ),
            onOpenSettings = {},
            onOpenTasbih = {},
            onOpenQibla = {},
            onToggleAudioMode = {},
            onTogglePrayerCompletion = {}
        )
    }
}

@Preview(name = "Main Dashboard Friday (Jumu'ah) Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainDashboardFridayPreview() {
    HayyaTheme {
        MainDashboardScreen(
            state = PrayerDashboardUiState(
                locationName = "Makkah, Saudi Arabia",
                nextPrayerType = PrayerType.DHUHR,
                nextPrayerName = "JUMU'AH",
                nextPrayerTimeFormatted = "12:15 PM",
                liveCountdownString = "00:45:00",
                progressRatio = 0.85f,
                hijriDateString = "18 Jumada al-Ula 1448 AH",
                isFriday = true,
                isSurahKahfRead = false,
                dailyPrayers = listOf(
                    PrayerTimeRowUiState(PrayerType.FAJR, "FAJR", 0L, "05:15 AM", isPast = true, isActive = false, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.SUNRISE, "SUNRISE", 0L, "06:30 AM", isPast = true, isActive = false, audioMode = AlertMode.GENTLE_BANNER),
                    PrayerTimeRowUiState(PrayerType.DHUHR, "JUMU'AH", 0L, "12:15 PM", isPast = false, isActive = true, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.ASR, "ASR", 0L, "03:45 PM", isPast = false, isActive = false, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.MAGHRIB, "MAGHRIB", 0L, "06:20 PM", isPast = false, isActive = false, audioMode = AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.ISHA, "ISHA", 0L, "07:50 PM", isPast = false, isActive = false, audioMode = AlertMode.MUTED)
                )
            ),
            onOpenSettings = {},
            onOpenTasbih = {},
            onOpenQibla = {},
            onToggleAudioMode = {},
            onTogglePrayerCompletion = {}
        )
    }
}
