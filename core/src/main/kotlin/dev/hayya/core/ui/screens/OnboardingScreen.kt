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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import dev.hayya.core.domain.model.MadhabOption
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
fun OnboardingScreen(
    locationName: String,
    selectedMadhab: MadhabOption,
    isBatteryOptimizationGranted: Boolean,
    isExactAlarmGranted: Boolean,
    onRequestLocation: () -> Unit,
    onSelectMadhab: (MadhabOption) -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onCompleteSetup: () -> Unit,
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "HAYYA",
                    style = TabularClockStyle.copy(
                        fontSize = 24.sp,
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmSandAccent
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Offline prayer time precision & reliable alarms",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Card 1: Location Setup
                OnboardingLocationCard(
                    locationName = locationName,
                    onClick = onRequestLocation
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card 2: Asr Madhab Selection
                OnboardingMadhabCard(
                    selectedMadhab = selectedMadhab,
                    onSelectMadhab = onSelectMadhab
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Card 3: Permissions (Battery & Exact Alarm)
                OnboardingPermissionsCard(
                    isBatteryOptimizationGranted = isBatteryOptimizationGranted,
                    isExactAlarmGranted = isExactAlarmGranted,
                    onRequestBatteryOptimization = onRequestBatteryOptimization,
                    onRequestExactAlarm = onRequestExactAlarm
                )
            }

            // Bottom Wide Pill Button: Complete Setup
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            ) {
                Button(
                    onClick = onCompleteSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmSandAccent,
                        contentColor = Color(0xFF141619)
                    )
                ) {
                    Text(
                        text = "COMPLETE SETUP",
                        style = TabularClockStyle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color(0xFF141619)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingLocationCard(
    locationName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(WarmSandAccentSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = WarmSandAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Location",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = locationName.ifEmpty { "Tap to confirm location" },
                    style = TabularClockStyle.copy(fontSize = 14.sp, color = TextPrimary)
                )
            }

            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Confirmed",
                tint = WarmSandAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OnboardingMadhabCard(
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
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "ASR JURISTIC SCHOOL",
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MadhabChoicePill(
                    label = "Standard (1x)",
                    isSelected = selectedMadhab == MadhabOption.STANDARD,
                    onClick = { onSelectMadhab(MadhabOption.STANDARD) },
                    modifier = Modifier.weight(1f)
                )
                MadhabChoicePill(
                    label = "Hanafi (2x)",
                    isSelected = selectedMadhab == MadhabOption.HANAFI,
                    onClick = { onSelectMadhab(MadhabOption.HANAFI) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MadhabChoicePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (isSelected) WarmSandAccent else Color.Transparent
    val textColor = if (isSelected) Color(0xFF141619) else TextSecondary
    val borderModifier = if (isSelected) {
        Modifier.border(1.dp, WarmSandAccent, RoundedCornerShape(8.dp))
    } else {
        Modifier.border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(borderModifier)
            .background(background)
            .clickable(onClick = onClick),
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

@Composable
private fun OnboardingPermissionsCard(
    isBatteryOptimizationGranted: Boolean,
    isExactAlarmGranted: Boolean,
    onRequestBatteryOptimization: () -> Unit,
    onRequestExactAlarm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "ALARM RELIABILITY",
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            PermissionRowItem(
                icon = Icons.Outlined.BatteryAlert,
                title = "Ignore Battery Optimizations",
                subtitle = "Guarantees alarm fires during Doze mode",
                isGranted = isBatteryOptimizationGranted,
                onClick = onRequestBatteryOptimization
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRowItem(
                icon = Icons.Outlined.Alarm,
                title = "Exact Alarm Permission",
                subtitle = "Ensures sub-second notification timing",
                isGranted = isExactAlarmGranted,
                onClick = onRequestExactAlarm
            )
        }
    }
}

@Composable
private fun PermissionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) WarmSandAccent else TextMuted,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = TabularClockStyle.copy(fontSize = 13.sp, color = TextPrimary)
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isGranted) WarmSandAccent else Color(0xFF1E2126)),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Granted",
                    tint = Color(0xFF141619),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview(name = "Onboarding Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingScreenPreview() {
    HayyaTheme {
        OnboardingScreen(
            locationName = "Makkah, Saudi Arabia",
            selectedMadhab = MadhabOption.STANDARD,
            isBatteryOptimizationGranted = true,
            isExactAlarmGranted = true,
            onRequestLocation = {},
            onSelectMadhab = {},
            onRequestBatteryOptimization = {},
            onRequestExactAlarm = {},
            onCompleteSetup = {}
        )
    }
}
