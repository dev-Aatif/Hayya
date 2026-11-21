package dev.hayya.core.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.CityPreset
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.prayerlog.DailyPrayerLog
import dev.hayya.core.domain.ramadan.IslamicDateInfo
import dev.hayya.core.ui.components.AlertModeBadge
import dev.hayya.core.ui.components.CityPickerBottomSheet
import dev.hayya.core.ui.components.PrayerCelestialIcon
import dev.hayya.core.ui.components.SolarSkyArc
import dev.hayya.core.ui.components.friendlyPrayerName
import dev.hayya.core.ui.components.prayerTimeOfDayHint
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TabularCountdownStyle
import dev.hayya.core.ui.theme.prayerAccentColor
import dev.hayya.core.ui.viewmodel.PrayerDashboardUiState
import dev.hayya.core.ui.viewmodel.PrayerTimeRowUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clean, calm, visual-first Home Dashboard.
 * Designed so anyone — from a 10-year-old to a 40-year-old — can check the next prayer,
 * mark prayers done, toggle alerts, or switch cities in one tap without reading walls of text.
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
    onOpenTracker: () -> Unit = {},
    onOpenTahajjud: () -> Unit = {},
    onOpenFridayGuide: () -> Unit = {},
    onSelectCity: (CityPreset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCityPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Clean Top Bar: Tappable City Selector + Date/Season Pill
            DashboardTopBar(
                locationName = state.locationName,
                hijriDateString = state.hijriDateString,
                islamicDateInfo = state.islamicDateInfo,
                isFriday = state.isFriday,
                onOpenCityPicker = { showCityPicker = true },
                onOpenRamadan = onOpenRamadan,
                onOpenFridayGuide = onOpenFridayGuide
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hero Celestial Countdown Card with Solar Sky Arc
            HeroCelestialCard(
                nextPrayerType = state.nextPrayerType,
                nextPrayerName = friendlyPrayerName(state.nextPrayerType, state.isFriday),
                countdown = state.liveCountdownString,
                timeFormatted = state.nextPrayerTimeFormatted,
                progressRatio = state.progressRatio,
                isAutoMuted = state.savedRingerState.isAutoMuted,
                autoMuteEndEpochMillis = state.savedRingerState.endEpochMillis,
                onRestoreRingerMode = onRestoreRingerMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Unified Today's Prayers Card (Inline Checkmarks + Celestial Icons + Clear Alert Pills)
            TodaysPrayersCard(
                prayers = state.dailyPrayers,
                todayLog = state.todayPrayerLog,
                isFriday = state.isFriday,
                onTogglePrayerCompletion = onTogglePrayerCompletion,
                onToggleAudioMode = onToggleAudioMode,
                onOpenTracker = onOpenTracker
            )

            // 4. Contextual Friday Strip (compact 1-tap access to full Friday Guide)
            if (state.isFriday) {
                Spacer(modifier = Modifier.height(12.dp))
                FridayQuickStrip(
                    isSurahKahfRead = state.isSurahKahfRead,
                    isAutoMuted = state.savedRingerState.isAutoMuted,
                    onOpenFridayGuide = onOpenFridayGuide,
                    onActivateKhutbahMute = onActivateKhutbahMute
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Compact Night & Fasting Quick Strip (opens full TahajjudScreen)
            NightFastingQuickStrip(
                state = state,
                onClick = onOpenTahajjud
            )

            // Clearance above the floating pill dock
            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    if (showCityPicker) {
        CityPickerBottomSheet(
            currentLocationName = state.locationName,
            onSelectCity = onSelectCity,
            onDismiss = { showCityPicker = false }
        )
    }
}

@Composable
private fun DashboardTopBar(
    locationName: String,
    hijriDateString: String,
    islamicDateInfo: IslamicDateInfo,
    isFriday: Boolean,
    onOpenCityPicker: () -> Unit,
    onOpenRamadan: () -> Unit,
    onOpenFridayGuide: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Tappable City Selector Pill
        Surface(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(CircleShape)
                .clickable(onClick = onOpenCityPicker),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Change city",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = locationName.substringBefore(","),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Seasonal Pill or Clean Hijri Date
        when {
            islamicDateInfo.isRamadan || islamicDateInfo.isEidAlFitr || islamicDateInfo.isEidAlAdha -> {
                val badgeText = when {
                    islamicDateInfo.isRamadan -> "Ramadan • Day ${islamicDateInfo.hijriDay}"
                    islamicDateInfo.isEidAlFitr -> "Eid al-Fitr"
                    else -> "Eid al-Adha"
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenRamadan)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
            isFriday -> {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenFridayGuide)
                ) {
                    Text(
                        text = "Jumu'ah",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
            hijriDateString.isNotEmpty() -> {
                Text(
                    text = hijriDateString.removeSuffix(" AH"),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeroCelestialCard(
    nextPrayerType: PrayerType,
    nextPrayerName: String,
    countdown: String,
    timeFormatted: String,
    progressRatio: Float,
    isAutoMuted: Boolean,
    autoMuteEndEpochMillis: Long,
    onRestoreRingerMode: () -> Unit
) {
    val accent = prayerAccentColor(nextPrayerType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Visual Sky Arc
                SolarSkyArc(
                    progress = progressRatio,
                    nextPrayerType = nextPrayerType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                )

                // Prayer Icon + Name Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrayerCelestialIcon(
                        prayerType = nextPrayerType,
                        size = 24.dp
                    )
                    Text(
                        text = "Next: $nextPrayerName",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Monumental Jitter-Free Countdown
                Text(
                    text = countdown,
                    style = TabularCountdownStyle.copy(
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Begins at $timeFormatted • ${prayerTimeOfDayHint(nextPrayerType)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (isAutoMuted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val endTime = remember(autoMuteEndEpochMillis) {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(autoMuteEndEpochMillis))
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onRestoreRingerMode)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Mosque Silence until $endTime • Tap to unmute",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaysPrayersCard(
    prayers: List<PrayerTimeRowUiState>,
    todayLog: DailyPrayerLog,
    isFriday: Boolean,
    onTogglePrayerCompletion: (PrayerType) -> Unit,
    onToggleAudioMode: (PrayerType) -> Unit,
    onOpenTracker: () -> Unit
) {
    val view = LocalView.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Card Header with Progress Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Prayers",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = if (todayLog.isComplete) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenTracker)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${todayLog.completedCount}/5 prayed",
                            style = TabularClockStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (todayLog.isComplete) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "View habits",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
            )

            prayers.forEachIndexed { index, prayer ->
                val isObligatory = prayer.prayerType.isObligatory
                val isCompleted = isObligatory && todayLog.isPrayerCompleted(prayer.prayerType)

                PrayerScheduleRow(
                    prayer = prayer,
                    displayName = friendlyPrayerName(prayer.prayerType, isFriday),
                    isObligatory = isObligatory,
                    isCompleted = isCompleted,
                    onToggleCompleted = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onTogglePrayerCompletion(prayer.prayerType)
                    },
                    onToggleAudioMode = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onToggleAudioMode(prayer.prayerType)
                    }
                )

                if (index < prayers.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f))
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerScheduleRow(
    prayer: PrayerTimeRowUiState,
    displayName: String,
    isObligatory: Boolean,
    isCompleted: Boolean,
    onToggleCompleted: () -> Unit,
    onToggleAudioMode: () -> Unit
) {
    val rowBg by animateColorAsState(
        targetValue = if (prayer.isActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            Color.Transparent
        },
        label = "prayer_row_bg"
    )

    val rowAlpha = if (prayer.isPast && !prayer.isActive && !isCompleted) 0.58f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .alpha(rowAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Tactile Prayer Checkmark + Celestial Icon + Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (isObligatory) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                        .border(
                            1.5.dp,
                            if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            CircleShape
                        )
                        .clickable(onClick = onToggleCompleted),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "$displayName prayed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // Non-obligatory Sunrise marker aligned neatly with checkmarks
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )
                }
            }

            PrayerCelestialIcon(
                prayerType = prayer.prayerType,
                size = 22.dp
            )

            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = if (prayer.isActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (prayer.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                )
                if (isObligatory && prayer.jamaatTimeFormatted.isNotEmpty()) {
                    Text(
                        text = "Jama'at ~${prayer.jamaatTimeFormatted}",
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Right: Formatted Time + Friendly Alert Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = prayer.timeFormatted,
                style = TabularClockStyle.copy(
                    fontSize = 15.sp,
                    fontWeight = if (prayer.isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (prayer.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )

            AlertModeBadge(
                mode = prayer.audioMode,
                onClick = onToggleAudioMode
            )
        }
    }
}

@Composable
private fun FridayQuickStrip(
    isSurahKahfRead: Boolean,
    isAutoMuted: Boolean,
    onOpenFridayGuide: () -> Unit,
    onActivateKhutbahMute: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenFridayGuide),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mosque,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Jumu'ah Checklist",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isSurahKahfRead) "Al-Kahf read ✓ • Tap for Salawat" else "Al-Kahf, Salawat & Khutbah mute",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (!isAutoMuted) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onActivateKhutbahMute)
                ) {
                    Text(
                        text = "Mute",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open Friday Guide",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun NightFastingQuickStrip(
    state: PrayerDashboardUiState,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (state.isTahajjudActiveNow) "Tahajjud Active Now" else "Night & Fasting",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Tahajjud ${state.tahajjudStartTimeFormatted} • Iftar ${state.iftarFormatted}",
                        style = TabularClockStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open Night & Fasting",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
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
                nextPrayerTimeFormatted = "03:45 PM",
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
