package dev.hayya.core.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.ui.theme.CardBorderColor
import dev.hayya.core.ui.theme.ElevatedCardSurface
import dev.hayya.core.ui.theme.OledBackground
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TextMuted
import dev.hayya.core.ui.theme.TextPrimary
import dev.hayya.core.ui.theme.TextSecondary
import dev.hayya.core.ui.theme.TrackBackground
import dev.hayya.core.ui.theme.WarmSandAccent
import dev.hayya.core.ui.theme.WarmSandAccentSubtle
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    isBatteryOptimizationIgnored: Boolean,
    onBackClick: () -> Unit,
    onSelectAutoSilence: (AutoSilenceTimeout) -> Unit,
    onSelectMadhab: (MadhabOption) -> Unit,
    onSelectCalculationMethod: (CalculationMethodOption) -> Unit,
    onToggleIshaMidnight: (Boolean) -> Unit,
    onTogglePreReminder: (Boolean) -> Unit,
    onToggleLastCall: (Boolean) -> Unit,
    onSelectHighLatitudeRule: (HighLatitudeRuleOption) -> Unit,
    onUpdatePrayerOffset: (PrayerType, Int) -> Unit,
    onLaunchBatterySettings: () -> Unit,
    onTriggerTestAlarm: (Int) -> Unit,
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Dashboard",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "SETTINGS",
                    style = TabularClockStyle.copy(
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SECTION 1: SYSTEM & BATTERY RELIABILITY
            // ==========================================
            SectionHeader(title = "1. SYSTEM & BATTERY RELIABILITY")
            Spacer(modifier = Modifier.height(8.dp))
            BatteryOptimizationCard(
                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                onLaunchBatterySettings = onLaunchBatterySettings
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // SECTION 2: TEST ALARM SIMULATOR
            // ==========================================
            SectionHeader(title = "2. ALARM TEST & SIMULATION")
            Spacer(modifier = Modifier.height(8.dp))
            TestAlarmCard(onTriggerTestAlarm = onTriggerTestAlarm)

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // SECTION 3: CALCULATION & JURISTICS
            // ==========================================
            SectionHeader(title = "3. CALCULATION CONVENTIONS & JURISTICS")
            Spacer(modifier = Modifier.height(8.dp))

            // Calculation Convention
            CalculationMethodSelectorCard(
                selectedMethod = userPreferences.calculationMethod,
                onSelectMethod = onSelectCalculationMethod
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Juristic Method (Asr)
            MadhabSelectorCard(
                selectedMadhab = userPreferences.madhab,
                onSelectMadhab = onSelectMadhab
            )

            Spacer(modifier = Modifier.height(14.dp))

            // High Latitude Rule
            HighLatitudeSelectorCard(
                selectedRule = userPreferences.highLatitudeRule,
                onSelectRule = onSelectHighLatitudeRule
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Isha Expiration Window
            IshaExpirationToggleCard(
                ishaExpiresAtMidnight = userPreferences.ishaExpiresAtMidnight,
                onToggle = onToggleIshaMidnight
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // SECTION 4: NOTIFICATIONS & ALERT TIERS
            // ==========================================
            SectionHeader(title = "4. ALERTS, TIERS & MINUTE OFFSETS")
            Spacer(modifier = Modifier.height(8.dp))

            // Auto-Silence Timeout Pills
            AutoSilenceCard(
                selectedTimeout = userPreferences.autoSilenceTimeout,
                onSelectTimeout = onSelectAutoSilence
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Alert Tiers
            AlertTiersCard(
                preReminderEnabled = userPreferences.preReminderEnabled,
                lastCallEnabled = userPreferences.lastCallEnabled,
                onTogglePreReminder = onTogglePreReminder,
                onToggleLastCall = onToggleLastCall
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Manual Minute Offsets
            ManualOffsetsCard(
                offsets = userPreferences.manualMinuteOffsets,
                onUpdateOffset = onUpdatePrayerOffset
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = TabularClockStyle.copy(
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            color = WarmSandAccent
        ),
        modifier = Modifier.padding(start = 4.dp)
    )
}

// ---------------------------------------------------------------------------
// Section 1: Battery Reliability Card
// ---------------------------------------------------------------------------
@Composable
private fun BatteryOptimizationCard(
    isBatteryOptimizationIgnored: Boolean,
    onLaunchBatterySettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onLaunchBatterySettings),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBatteryOptimizationIgnored) WarmSandAccentSubtle else Color(0x33FF6B6B)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBatteryOptimizationIgnored) Icons.Outlined.Shield else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (isBatteryOptimizationIgnored) WarmSandAccent else Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBatteryOptimizationIgnored) "Unrestricted Background Active" else "Battery Restriction Detected",
                        style = TabularClockStyle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = if (isBatteryOptimizationIgnored) "Adhan alarms protected from Doze kills" else "Action required: choose 'Unrestricted'",
                        fontSize = 12.sp,
                        color = if (isBatteryOptimizationIgnored) WarmSandAccent else Color(0xFFFF8585)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step-by-step guidance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TrackBackground)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "HOW TO CONFIGURE UNRESTRICTED BATTERY:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Tap the button below to open App Battery Usage.\n2. Select 'Unrestricted' instead of 'Optimized'.\n3. This guarantees Adhan calls wake the phone even in deep sleep.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLaunchBatterySettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBatteryOptimizationIgnored) TrackBackground else WarmSandAccent,
                    contentColor = if (isBatteryOptimizationIgnored) WarmSandAccent else Color(0xFF141619)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isBatteryOptimizationIgnored) "Manage Battery Settings" else "Open App Info > Battery Directly",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section 2: Test Alarm Simulation
// ---------------------------------------------------------------------------
@Composable
private fun TestAlarmCard(onTriggerTestAlarm: (Int) -> Unit) {
    var selectedDelay by remember { mutableIntStateOf(15) }
    var countdownRemaining by remember { mutableIntStateOf(-1) }

    LaunchedEffect(countdownRemaining) {
        if (countdownRemaining > 0) {
            delay(1000L)
            countdownRemaining -= 1
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AccessAlarm,
                    contentDescription = null,
                    tint = WarmSandAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Test Alarm Simulator",
                    style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Experience how the real lockscreen alarm, audio, and tactile dismiss slider work. Pick a timer, tap trigger, and lock your screen.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Timer pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 15, 30, 60).forEach { seconds ->
                    val isSelected = selectedDelay == seconds
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) WarmSandAccent else TrackBackground)
                            .clickable { selectedDelay = seconds }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${seconds}s",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF141619) else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (countdownRemaining > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmSandAccentSubtle)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Alarm triggers in ${countdownRemaining}s... Lock your screen now!",
                        color = WarmSandAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                Button(
                    onClick = {
                        countdownRemaining = selectedDelay
                        onTriggerTestAlarm(selectedDelay)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmSandAccent,
                        contentColor = Color(0xFF141619)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trigger Test Alarm (${selectedDelay}s)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section 3: Calculation Conventions (Full Uncut Descriptions)
// ---------------------------------------------------------------------------
@Composable
private fun CalculationMethodSelectorCard(
    selectedMethod: CalculationMethodOption,
    onSelectMethod: (CalculationMethodOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Calculation Authority / Method",
                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Defines astronomical twilight angles for Fajr and Isha calculations.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Currently Selected Method Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TrackBackground)
                    .clickable { expanded = !expanded }
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedMethod.displayName,
                            style = TabularClockStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmSandAccent
                            )
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = WarmSandAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // FULL UNTRUNCATED DESCRIPTION
                    Text(
                        text = selectedMethod.description,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Canonical reference: ${selectedMethod.defaultCity}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Expandable List of All Methods
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    CalculationMethodOption.entries.forEach { method ->
                        val isSelected = method == selectedMethod
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) WarmSandAccentSubtle else TrackBackground)
                                .clickable {
                                    onSelectMethod(method)
                                    expanded = false
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = method.displayName,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) WarmSandAccent else TextPrimary
                                        )
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = WarmSandAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // FULL COMPLETE UNTRUNCATED TEXT
                                Text(
                                    text = method.description,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Juristic Madhab Selector (Asr)
// ---------------------------------------------------------------------------
@Composable
private fun MadhabSelectorCard(
    selectedMadhab: MadhabOption,
    onSelectMadhab: (MadhabOption) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Asr Juristic Method (Madhab)",
                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Determines shadow length multiplier used to calculate Asr start time.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            MadhabOption.entries.forEach { option ->
                val isSelected = option == selectedMadhab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) WarmSandAccentSubtle else TrackBackground)
                        .clickable { onSelectMadhab(option) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.displayName,
                                style = TabularClockStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) WarmSandAccent else TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = option.description,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = TextSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = WarmSandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// High Latitude Rule Selector
// ---------------------------------------------------------------------------
@Composable
private fun HighLatitudeSelectorCard(
    selectedRule: HighLatitudeRuleOption,
    onSelectRule: (HighLatitudeRuleOption) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "High Latitude Twilight Rule",
                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Adjusts Fajr and Isha when solar twilight persists continuously through summer nights.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            HighLatitudeRuleOption.entries.forEach { rule ->
                val isSelected = rule == selectedRule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) WarmSandAccentSubtle else TrackBackground)
                        .clickable { onSelectRule(rule) }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rule.displayName,
                                style = TabularClockStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) WarmSandAccent else TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = rule.description,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = TextSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = WarmSandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Isha Expiration Toggle
// ---------------------------------------------------------------------------
@Composable
private fun IshaExpirationToggleCard(
    ishaExpiresAtMidnight: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Isha Window Expiration",
                    style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (ishaExpiresAtMidnight) {
                        "Window expires at Islamic Midnight (halfway between Maghrib and Fajr)."
                    } else {
                        "Window continues until the start of next day Fajr."
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = ishaExpiresAtMidnight,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WarmSandAccent,
                    checkedTrackColor = WarmSandAccentSubtle,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = TrackBackground
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Auto-Silence Timeout Pills
// ---------------------------------------------------------------------------
@Composable
private fun AutoSilenceCard(
    selectedTimeout: AutoSilenceTimeout,
    onSelectTimeout: (AutoSilenceTimeout) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Auto-Silence Duration",
                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Maximum audio playback duration before alarm smoothly fades out.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutoSilenceTimeout.entries.forEach { timeout ->
                    val isSelected = timeout == selectedTimeout
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) WarmSandAccent else TrackBackground)
                            .clickable { onSelectTimeout(timeout) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (timeout) {
                                AutoSilenceTimeout.SECONDS_30 -> "30s"
                                AutoSilenceTimeout.SECONDS_60 -> "1m"
                                AutoSilenceTimeout.SECONDS_120 -> "2m"
                                AutoSilenceTimeout.FULL_AUDIO -> "Full"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF141619) else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Alert Tiers Card (Pre-reminder, Main Call, Last Call)
// ---------------------------------------------------------------------------
@Composable
private fun AlertTiersCard(
    preReminderEnabled: Boolean,
    lastCallEnabled: Boolean,
    onTogglePreReminder: (Boolean) -> Unit,
    onToggleLastCall: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "3-Tier Notification Schedule",
                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Customise which reminders dispatch for each prayer window.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tier 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tier 1: Pre-Reminder (T - 15m)",
                        style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Gentle heads-up notification 15 minutes before prayer time",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = preReminderEnabled,
                    onCheckedChange = onTogglePreReminder,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WarmSandAccent,
                        checkedTrackColor = WarmSandAccentSubtle
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tier 2 (Locked on)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tier 2: Main Call (T0 Start Time)",
                        style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Full Adhan alarm audio & lockscreen wake screen",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "ALWAYS ON",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmSandAccent,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tier 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tier 3: Last Call (Expiry - 20m)",
                        style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Urgent reminder 20 minutes before window closes",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = lastCallEnabled,
                    onCheckedChange = onToggleLastCall,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WarmSandAccent,
                        checkedTrackColor = WarmSandAccentSubtle
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Manual Minute Offsets Stepper Card
// ---------------------------------------------------------------------------
@Composable
private fun ManualOffsetsCard(
    offsets: Map<PrayerType, Int>,
    onUpdateOffset: (PrayerType, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Manual Prayer Time Offsets",
                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add or subtract minutes (-30 to +30) to match your local mosque.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrayerType.entries.forEach { prayer ->
                val currentOffset = offsets[prayer] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prayer.name,
                        style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Minus button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TrackBackground)
                                .clickable {
                                    if (currentOffset > -30) onUpdateOffset(prayer, currentOffset - 1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "−", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = if (currentOffset > 0) "+${currentOffset}m" else "${currentOffset}m",
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentOffset != 0) WarmSandAccent else TextSecondary
                            ),
                            modifier = Modifier.width(42.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Plus button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TrackBackground)
                                .clickable {
                                    if (currentOffset < 30) onUpdateOffset(prayer, currentOffset + 1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
