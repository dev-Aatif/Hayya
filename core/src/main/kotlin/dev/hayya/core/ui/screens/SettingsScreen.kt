package dev.hayya.core.ui.screens

import android.content.res.Configuration
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.ui.theme.CardBorderColor
import dev.hayya.core.ui.theme.ElevatedCardSurface
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.OledBackground
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TextMuted
import dev.hayya.core.ui.theme.TextPrimary
import dev.hayya.core.ui.theme.TextSecondary
import dev.hayya.core.ui.theme.WarmSandAccent
import dev.hayya.core.ui.theme.WarmSandAccentSubtle

@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    isBatteryOptimizationIgnored: Boolean,
    onBackClick: () -> Unit,
    onSelectAutoSilence: (AutoSilenceTimeout) -> Unit,
    onSelectMadhab: (MadhabOption) -> Unit,
    onSelectCalculationMethod: (CalculationMethodOption) -> Unit,
    onToggleIshaMidnight: (Boolean) -> Unit,
    onLaunchBatterySettings: () -> Unit,
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
                        contentDescription = "Back",
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

            // Section 1: System Reliability Shield
            SystemReliabilityCard(
                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                onLaunchBatterySettings = onLaunchBatterySettings
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Auto-Silence Timeout Pills
            SectionHeader(title = "AUTO-SILENCE TIMEOUT")
            Spacer(modifier = Modifier.height(8.dp))
            AutoSilencePillSelector(
                selectedTimeout = userPreferences.autoSilenceTimeout,
                onSelectTimeout = onSelectAutoSilence
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Juristic Method (Asr Shadow)
            SectionHeader(title = "ASR JURISTIC SCHOOL (MADHAB)")
            Spacer(modifier = Modifier.height(8.dp))
            MadhabSelectorCard(
                selectedMadhab = userPreferences.madhab,
                onSelectMadhab = onSelectMadhab
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Calculation Convention
            SectionHeader(title = "CALCULATION CONVENTION")
            Spacer(modifier = Modifier.height(8.dp))
            CalculationMethodSelector(
                selectedMethod = userPreferences.calculationMethod,
                onSelectMethod = onSelectCalculationMethod
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 5: Expiration Rules
            SectionHeader(title = "PRAYER WINDOW RULES")
            Spacer(modifier = Modifier.height(8.dp))
            IshaExpirationToggleCard(
                ishaExpiresAtMidnight = userPreferences.ishaExpiresAtMidnight,
                onToggle = onToggleIshaMidnight
            )

            Spacer(modifier = Modifier.height(36.dp))
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
            color = TextSecondary
        ),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SystemReliabilityCard(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBatteryOptimizationIgnored) "Alarm Protection Active" else "Battery Restriction Detected",
                    style = TabularClockStyle.copy(fontSize = 14.sp, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBatteryOptimizationIgnored) {
                        "Exempt from OS background kills and deep Doze pauses."
                    } else {
                        "Tap to whitelist and enable exact alarm reliability."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AutoSilencePillSelector(
    selectedTimeout: AutoSilenceTimeout,
    onSelectTimeout: (AutoSilenceTimeout) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf(
            AutoSilenceTimeout.SECONDS_30 to "30s",
            AutoSilenceTimeout.SECONDS_60 to "1m",
            AutoSilenceTimeout.SECONDS_120 to "2m",
            AutoSilenceTimeout.FULL_AUDIO to "Full"
        )

        options.forEach { (timeout, label) ->
            val isSelected = selectedTimeout == timeout
            val background = if (isSelected) WarmSandAccent else ElevatedCardSurface
            val textColor = if (isSelected) Color(0xFF141619) else TextSecondary
            val borderModifier = if (isSelected) {
                Modifier.border(1.dp, WarmSandAccent, RoundedCornerShape(10.dp))
            } else {
                Modifier.border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(borderModifier)
                    .background(background)
                    .clickable { onSelectTimeout(timeout) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = TabularClockStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                )
            }
        }
    }
}

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
        Column(modifier = Modifier.padding(12.dp)) {
            MadhabOptionItem(
                title = "Standard (Shafi'i, Maliki, Hanbali)",
                subtitle = "Shadow equals object length (1x)",
                isSelected = selectedMadhab == MadhabOption.STANDARD,
                onClick = { onSelectMadhab(MadhabOption.STANDARD) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            MadhabOptionItem(
                title = "Hanafi",
                subtitle = "Shadow equals twice object length (2x)",
                isSelected = selectedMadhab == MadhabOption.HANAFI,
                onClick = { onSelectMadhab(MadhabOption.HANAFI) }
            )
        }
    }
}

@Composable
private fun MadhabOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TabularClockStyle.copy(
                    fontSize = 14.sp,
                    color = if (isSelected) WarmSandAccent else TextPrimary
                )
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = WarmSandAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CalculationMethodSelector(
    selectedMethod: CalculationMethodOption,
    onSelectMethod: (CalculationMethodOption) -> Unit
) {
    val commonMethods = listOf(
        CalculationMethodOption.MUSLIM_WORLD_LEAGUE,
        CalculationMethodOption.UMM_AL_QURA,
        CalculationMethodOption.NORTH_AMERICA,
        CalculationMethodOption.EGYPTIAN,
        CalculationMethodOption.KARACHI,
        CalculationMethodOption.DUBAI
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            commonMethods.forEach { method ->
                val isSelected = selectedMethod == method
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectMethod(method) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = method.displayName,
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                color = if (isSelected) WarmSandAccent else TextPrimary
                            )
                        )
                        Text(
                            text = method.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            tint = WarmSandAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

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
                    text = "Isha Window Ends at Islamic Midnight",
                    style = TabularClockStyle.copy(fontSize = 14.sp, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Calculated as exact midpoint between Maghrib and Dawn.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = ishaExpiresAtMidnight,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WarmSandAccent,
                    checkedTrackColor = ElevatedCardSurface,
                    checkedBorderColor = WarmSandAccent,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = TrackBackground,
                    uncheckedBorderColor = CardBorderColor
                )
            )
        }
    }
}

@Preview(name = "Settings Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    HayyaTheme {
        SettingsScreen(
            userPreferences = UserPreferences(),
            isBatteryOptimizationIgnored = true,
            onBackClick = {},
            onSelectAutoSilence = {},
            onSelectMadhab = {},
            onSelectCalculationMethod = {},
            onToggleIshaMidnight = {},
            onLaunchBatterySettings = {}
        )
    }
}
