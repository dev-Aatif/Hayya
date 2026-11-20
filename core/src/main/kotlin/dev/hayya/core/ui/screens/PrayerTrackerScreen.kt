package dev.hayya.core.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.prayerlog.DailyPrayerLog
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS
import dev.hayya.core.domain.prayerlog.RollingPrayerHistory
import dev.hayya.core.ui.components.PrayerCelestialIcon
import dev.hayya.core.ui.components.SectionPageHeader
import dev.hayya.core.ui.components.friendlyPrayerName
import dev.hayya.core.ui.components.prayerTimeOfDayHint
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.viewmodel.PrayerDashboardUiState
import dev.hayya.core.ui.viewmodel.PrayerTimeRowUiState
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

/**
 * Dedicated visual Prayer Habits & 7-Day Consistency page.
 */
@Composable
fun PrayerTrackerScreen(
    state: PrayerDashboardUiState,
    onBackClick: () -> Unit,
    onTogglePrayerCompletion: (PrayerType) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val view = LocalView.current
    val dailyLog = state.todayPrayerLog
    val history = state.rollingPrayerHistory

    val animatedProgress by animateFloatAsState(
        targetValue = (dailyLog.completedCount.toFloat() / dailyLog.totalCount.coerceAtLeast(1)),
        animationSpec = tween(durationMillis = 500),
        label = "tracker_progress"
    )

    val statusHeadline = when (dailyLog.completedCount) {
        0 -> "Ready for today's prayers"
        5 -> "All 5 prayers completed!"
        else -> "${5 - dailyLog.completedCount} remaining today"
    }

    val statusSub = when (dailyLog.completedCount) {
        0 -> "Tap any prayer below as you finish praying."
        5 -> "MashaAllah — keep your steady rhythm going."
        else -> "You've prayed ${dailyLog.completedCount} out of 5 prayers today."
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionPageHeader(
                title = "Prayer Habits",
                subtitle = state.hijriDateString.ifEmpty { "Daily tracker & 7-day view" },
                onBackClick = onBackClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hero Visual Ring Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(22.dp)
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(92.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(92.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${dailyLog.completedCount}/5",
                                style = TabularClockStyle.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusHeadline,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusSub,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val weeklyPct = (history.overallRatio * 100).roundToInt()
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "7-Day Score: $weeklyPct%",
                                style = TabularClockStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Today's 5 Prayers",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            val timeMap = state.dailyPrayers.associateBy { it.prayerType }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OBLIGATORY_PRAYERS.forEach { prayer ->
                    val isDone = dailyLog.isPrayerCompleted(prayer)
                    val timeStr = timeMap[prayer]?.timeFormatted ?: "--:--"
                    val cardBg by animateColorAsState(
                        targetValue = if (isDone) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        label = "prayer_check_bg"
                    )
                    val borderColor = if (isDone) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onTogglePrayerCompletion(prayer)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                PrayerCelestialIcon(prayerType = prayer, size = 28.dp)
                                Column {
                                    Text(
                                        text = friendlyPrayerName(prayer, state.isFriday),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "${prayerTimeOfDayHint(prayer)} • $timeStr",
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Prayed",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isDone) "Prayed" else "Log",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (history.dailyLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Last 7 Days",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val logs = history.dailyLogs.takeLast(7)
                        logs.forEachIndexed { index, log ->
                            val isToday = index == logs.lastIndex
                            val dayLabel = if (isToday) {
                                "Today"
                            } else {
                                log.date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                // 5 visual prayer dots for that day
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    OBLIGATORY_PRAYERS.forEach { prayer ->
                                        val completed = log.isPrayerCompleted(prayer)
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (completed) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (completed) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }

                                Text(
                                    text = "${log.completedCount}/5",
                                    style = TabularClockStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview(name = "Prayer Tracker Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrayerTrackerPreview() {
    HayyaTheme {
        PrayerTrackerScreen(
            state = PrayerDashboardUiState(
                hijriDateString = "14 Jumada al-Ula 1448 AH",
                todayPrayerLog = DailyPrayerLog(
                    date = LocalDate(2026, 11, 20),
                    completedPrayers = setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR)
                ),
                rollingPrayerHistory = RollingPrayerHistory(
                    dailyLogs = listOf(
                        DailyPrayerLog(LocalDate(2026, 11, 14), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)),
                        DailyPrayerLog(LocalDate(2026, 11, 15), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.MAGHRIB, PrayerType.ISHA)),
                        DailyPrayerLog(LocalDate(2026, 11, 16), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)),
                        DailyPrayerLog(LocalDate(2026, 11, 17), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR)),
                        DailyPrayerLog(LocalDate(2026, 11, 18), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)),
                        DailyPrayerLog(LocalDate(2026, 11, 19), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB)),
                        DailyPrayerLog(LocalDate(2026, 11, 20), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR))
                    )
                ),
                dailyPrayers = listOf(
                    PrayerTimeRowUiState(PrayerType.FAJR, "FAJR", 0L, "05:15 AM", true, false, AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.DHUHR, "DHUHR", 0L, "12:15 PM", true, false, AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.ASR, "ASR", 0L, "03:45 PM", false, true, AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.MAGHRIB, "MAGHRIB", 0L, "06:20 PM", false, false, AlertMode.FULL_ALARM),
                    PrayerTimeRowUiState(PrayerType.ISHA, "ISHA", 0L, "07:50 PM", false, false, AlertMode.FULL_ALARM)
                )
            ),
            onBackClick = {},
            onTogglePrayerCompletion = {}
        )
    }
}
