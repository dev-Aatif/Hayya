package dev.hayya.core.ui.screens

import android.os.Build
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.PostAdhanDua
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.viewmodel.PrayerDashboardUiState

/**
 * Unified Spiritual Companion & Diagnostics Directory.
 *
 * Engineered with Modern Utilitarian Minimalism: clean editorial hierarchy,
 * precise 1px micro-borders, flat bento architecture, and zero marketing clutter.
 */
@Composable
fun ToolsScreen(
    state: PrayerDashboardUiState,
    onOpenCompass: () -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenRamadan: () -> Unit = {},
    onIncrementTasbih: () -> Unit = {},
    onTogglePrayerCompletion: (PrayerType) -> Unit = {},
    onTogglePostAdhanDua: (Boolean) -> Unit,
    onToggleTahajjudAlarm: (Boolean) -> Unit = {},
    onLaunchBatterySettings: () -> Unit,
    onTriggerTestAlarm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showTahajjudSheet by remember { mutableStateOf(false) }
    var showPostAdhanDuaSheet by remember { mutableStateOf(false) }
    var showAlarmSimulatorSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Page Header: Pure Utilitarian Editorial Typography
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "COMPANION",
                        style = TabularClockStyle.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Spiritual utilities & device reliability suite",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "OFFLINE",
                        style = TabularClockStyle.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1 Header: Spiritual Utilities
            SectionHeader(title = "SPIRITUAL UTILITIES")

            Spacer(modifier = Modifier.height(10.dp))

            // Item 1: Night Windows & Tahajjud Engine
            val tahajjudBadge = if (state.isTahajjudActiveNow) {
                "Active Now"
            } else {
                "In ${state.tahajjudCountdown}"
            }

            CompanionDirectoryItem(
                title = "Tahajjud & Night Windows",
                description = "Islamic midnight (Nisf al-Layl), Last Third of the Night (Thuluth al-Akhir), and Sunnah fasting guide.",
                icon = Icons.Outlined.Bedtime,
                badge = tahajjudBadge,
                onClick = { showTahajjudSheet = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Item 2: Ramadan & Eid Hub Navigation Item
            val ramadanBadge = if (state.islamicDateInfo.isRamadan) {
                "Day ${state.islamicDateInfo.hijriDay} • Active"
            } else if (state.islamicDateInfo.isEidAlFitr || state.islamicDateInfo.isEidAlAdha) {
                "Eid Celebration"
            } else {
                "Suhoor, Iftar & Sawm"
            }

            CompanionDirectoryItem(
                title = "Ramadan & Eid Hub",
                description = "Live Suhoor & Iftar countdown, 30-day fast tracker, authentic Duas, and Eid celebrations.",
                icon = Icons.Outlined.Star,
                badge = ramadanBadge,
                onClick = onOpenRamadan
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Item 3: Qibla Compass Navigation Item
            CompanionDirectoryItem(
                title = "Qibla Compass",
                description = "Offline spherical great-circle bearing to Kaaba with ±2° haptic alignment.",
                icon = Icons.Outlined.Explore,
                badge = state.qiblaReadout.ifEmpty { "118° ESE" },
                onClick = onOpenCompass
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Item 4: Tactile Tasbih Navigation Item
            val targetLabel = state.tasbihMode.defaultTarget?.toString() ?: "Free"
            CompanionDirectoryItem(
                title = "Tactile Tasbih",
                description = "Full-screen minimalist OLED counter with instant haptic ticks and milestone pulses.",
                icon = Icons.Outlined.TouchApp,
                badge = "${state.tasbihCount} / $targetLabel",
                onClick = onOpenTasbih
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Item 5: Post-Adhan Dua Navigation Item
            CompanionDirectoryItem(
                title = "Post-Adhan Dua",
                description = "Authentic supplication recited after the call to prayer (Sahih al-Bukhari 614).",
                icon = Icons.Default.MenuBook,
                badge = if (state.userPreferences.showPostAdhanDua) "Active on Wake" else null,
                onClick = { showPostAdhanDuaSheet = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2 Header: Reliability & Diagnostics
            SectionHeader(title = "SYSTEM & RELIABILITY")

            Spacer(modifier = Modifier.height(10.dp))

            // Item 6: System Reliability & OEM Settings Item
            CompanionDirectoryItem(
                title = "System Reliability & OEM",
                description = "Exempt Hayya from OEM task killers to guarantee exact alarm delivery.",
                icon = Icons.Default.Shield,
                badge = Build.MANUFACTURER.uppercase(),
                onClick = onLaunchBatterySettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Item 7: Test Alarm Simulator Navigation Item
            CompanionDirectoryItem(
                title = "Alarm Simulator",
                description = "Test the lockscreen wake screen, Adhan audio playback, gradual volume ramp, and tactile dismissal.",
                icon = Icons.Default.Alarm,
                onClick = { showAlarmSimulatorSheet = true }
            )

            // Bottom padding to ensure floating navbar never obscures content
            Spacer(modifier = Modifier.height(112.dp))
        }
    }

    // Modal Bottom Sheet: Tahajjud & Night Calculations
    if (showTahajjudSheet) {
        TahajjudModalSheet(
            state = state,
            onToggleTahajjudAlarm = onToggleTahajjudAlarm,
            onDismiss = { showTahajjudSheet = false }
        )
    }

    // Modal Bottom Sheet: Authentic Post-Adhan Dua
    if (showPostAdhanDuaSheet) {
        PostAdhanDuaModalSheet(
            showOnWake = state.userPreferences.showPostAdhanDua,
            onToggleShowOnWake = onTogglePostAdhanDua,
            onDismiss = { showPostAdhanDuaSheet = false }
        )
    }

    // Modal Bottom Sheet: Alarm Engine & Wake Simulator
    if (showAlarmSimulatorSheet) {
        AlarmSimulatorModalSheet(
            onTriggerTestAlarm = onTriggerTestAlarm,
            onDismiss = { showAlarmSimulatorSheet = false }
        )
    }
}

/**
 * Editorial section header adhering strictly to minimal typographic contrast.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = TabularClockStyle.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            color = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Minimalist directory option item: Title, short description, and arrow mark.
 * Conforms strictly to 12dp crisp radius, 1px subtle borders, and pill badges.
 */
@Composable
private fun CompanionDirectoryItem(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Leading Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (badge != null) {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = badge,
                                    style = TabularClockStyle.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Forward Chevron Indicator
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Modal bottom sheet detailing the Last Third of the Night calculation,
 * Islamic Midnight, Sunnah fasting virtues, and gentle pre-alarm configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TahajjudModalSheet(
    state: PrayerDashboardUiState,
    onToggleTahajjudAlarm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NIGHT & TAHAJJUD ENGINE",
                    style = TabularClockStyle.copy(
                        fontSize = 12.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "BUKHARI 1145",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4-Point Timeline Bento Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NightMetricCard(
                    label = "SUNSET (START)",
                    time = state.iftarFormatted,
                    subtitle = "Maghrib",
                    modifier = Modifier.weight(1f)
                )
                NightMetricCard(
                    label = "MIDNIGHT",
                    time = state.islamicMidnightFormatted,
                    subtitle = "Nisf al-Layl",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NightMetricCard(
                    label = "LAST THIRD",
                    time = state.tahajjudStartTimeFormatted,
                    subtitle = "Thuluth al-Akhir",
                    modifier = Modifier.weight(1f),
                    isHighlighted = true
                )
                NightMetricCard(
                    label = "TRUE DAWN (END)",
                    time = state.suhoorCutoffFormatted,
                    subtitle = "Fajr / Cutoff",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Authentic Hadith Citation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Prophetic Virtue of the Last Third",
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "“Our Lord, the Blessed and Exalted, descends every night to the lowest heaven when the last third of the night remains, saying: 'Who calls upon Me that I may answer him? Who asks of Me that I may give him? Who seeks My forgiveness that I may forgive him?'”",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sahih al-Bukhari 1145 • Sahih Muslim 758",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voluntary Fasting Guidance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Sunnah Fasting Schedule",
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Mondays & Thursdays: Prophetic weekly fasts when deeds are presented.\n• Ayyam al-Beed (The White Days): 13th, 14th, and 15th of each lunar Hijri month.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gentle Pre-Alarm Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gentle Tahajjud Pre-Alarm",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Trigger exact alarm at the start of the Last Third",
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
    }
}

@Composable
private fun NightMetricCard(
    label: String,
    time: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val borderColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = TabularClockStyle.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                style = TabularClockStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}

/**
 * Full-screen modal bottom sheet presenting the authentic Sahih al-Bukhari 614 Post-Adhan Dua.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostAdhanDuaModalSheet(
    showOnWake: Boolean,
    onToggleShowOnWake: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PostAdhanDua.TITLE,
                    style = TabularClockStyle.copy(
                        fontSize = 12.sp,
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = PostAdhanDua.REFERENCE,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = PostAdhanDua.ARABIC_TEXT,
                        fontSize = 20.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = PostAdhanDua.TRANSLITERATION,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "“${PostAdhanDua.TRANSLATION}”",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = PostAdhanDua.VIRTUE,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Display on Lockscreen Wake",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Shows supplication card upon dismiss on lockscreen",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = showOnWake,
                    onCheckedChange = onToggleShowOnWake,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * Modal bottom sheet for triggering instant and countdown-based test alarms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmSimulatorModalSheet(
    onTriggerTestAlarm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ALARM SIMULATOR",
                style = TabularClockStyle.copy(
                    fontSize = 13.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Test how Hayya presents the full-screen wake screen, streams Adhan audio with gradual volume escalation, and responds to tactile dismissal. For lockscreen testing, use the 10-second countdown and lock your device immediately.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onTriggerTestAlarm(0)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Instant (0s)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = {
                        onTriggerTestAlarm(10)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "10s Countdown",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
