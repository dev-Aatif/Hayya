package dev.hayya.core.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.ui.theme.CardBorderColor
import dev.hayya.core.ui.theme.ElevatedCardSurface
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.HeroLabelStyle
import dev.hayya.core.ui.theme.OledBackground
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TabularCountdownStyle
import dev.hayya.core.ui.theme.TextMuted
import dev.hayya.core.ui.theme.TextPrimary
import dev.hayya.core.ui.theme.TextSecondary
import dev.hayya.core.ui.theme.WarmSandAccent
import dev.hayya.core.ui.theme.WarmSandAccentSubtle
import dev.hayya.core.ui.viewmodel.PrayerDashboardUiState
import dev.hayya.core.ui.viewmodel.PrayerTimeRowUiState

@Composable
fun MainDashboardScreen(
    state: PrayerDashboardUiState,
    onOpenSettings: () -> Unit,
    onToggleAudioMode: (PrayerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = OledBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            DashboardTopBar(
                locationName = state.locationName,
                onOpenSettings = onOpenSettings
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Hero Section: Next Prayer Countdown
            HeroCountdownSection(
                nextPrayerName = state.nextPrayerName,
                countdown = state.liveCountdownString,
                timeFormatted = state.nextPrayerTimeFormatted,
                progressRatio = state.progressRatio
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Timetable Section
            TimetableSection(
                prayers = state.dailyPrayers,
                onToggleAudioMode = onToggleAudioMode
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Section: Hijri Date
            if (state.hijriDateString.isNotEmpty()) {
                HijriDateBadge(hijriDateString = state.hijriDateString)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DashboardTopBar(
    locationName: String,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = locationName.uppercase(),
                style = TabularClockStyle.copy(
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = TextSecondary
                )
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
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
        Text(
            text = nextPrayerName,
            style = HeroLabelStyle
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = countdown,
            style = TabularCountdownStyle
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Begins at $timeFormatted",
            fontSize = 15.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Subtle horizontal progress bar showing window ratio
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .width(180.dp)
                .height(3.dp)
                .clip(CircleShape),
            color = WarmSandAccent,
            trackColor = CardBorderColor
        )
    }
}

@Composable
private fun TimetableSection(
    prayers: List<PrayerTimeRowUiState>,
    onToggleAudioMode: (PrayerType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prayers.forEach { prayer ->
            PrayerRowItem(
                prayer = prayer,
                onToggleAudioMode = { onToggleAudioMode(prayer.prayerType) }
            )
        }
    }
}

@Composable
private fun PrayerRowItem(
    prayer: PrayerTimeRowUiState,
    onToggleAudioMode: () -> Unit
) {
    val isElevated = prayer.isActive
    val cardBackground = if (isElevated) ElevatedCardSurface else Color.Transparent
    val borderModifier = if (isElevated) {
        Modifier.border(1.dp, WarmSandAccentSubtle, RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, CardBorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    }

    val contentAlpha = if (prayer.isPast && !prayer.isActive) 0.4f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .alpha(contentAlpha),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prayer.isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(WarmSandAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = prayer.name,
                    style = TabularClockStyle.copy(
                        fontWeight = if (prayer.isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (prayer.isActive) TextPrimary else TextSecondary
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
                        color = if (prayer.isActive) WarmSandAccent else TextPrimary
                    )
                )

                // Inline audio mode selector
                AudioModeIcon(
                    mode = prayer.audioMode,
                    onClick = onToggleAudioMode
                )
            }
        }
    }
}

@Composable
private fun AudioModeIcon(
    mode: AlertMode,
    onClick: () -> Unit
) {
    val (icon, tint) = when (mode) {
        AlertMode.FULL_ALARM -> Icons.Outlined.NotificationsActive to WarmSandAccent
        AlertMode.GENTLE_BANNER -> Icons.Outlined.Notifications to TextSecondary
        AlertMode.MUTED -> Icons.Outlined.NotificationsOff to TextMuted
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

@Composable
private fun HijriDateBadge(
    hijriDateString: String
) {
    Card(
        modifier = Modifier.border(1.dp, CardBorderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Text(
            text = hijriDateString,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            style = TabularClockStyle.copy(
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        )
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
            onToggleAudioMode = {}
        )
    }
}
