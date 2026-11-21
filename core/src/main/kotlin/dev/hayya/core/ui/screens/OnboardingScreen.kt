package dev.hayya.core.ui.screens

import android.content.res.Configuration
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.CityPreset
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.ui.components.CityPickerBottomSheet
import dev.hayya.core.ui.components.PrayerCelestialIcon
import dev.hayya.core.ui.theme.HayyaSuccess
import dev.hayya.core.ui.theme.HayyaTheme

/**
 * Simple, friendly 3-step welcome setup for choosing your city, Asr time, and alarm permissions.
 */
@Composable
fun OnboardingScreen(
    locationName: String,
    selectedMadhab: MadhabOption,
    isBatteryOptimizationGranted: Boolean,
    isExactAlarmGranted: Boolean,
    isNotificationGranted: Boolean = true,
    isFullScreenIntentGranted: Boolean = true,
    onSelectCity: (CityPreset) -> Unit,
    onSelectMadhab: (MadhabOption) -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestFullScreenIntent: () -> Unit = {},
    onCompleteSetup: () -> Unit,
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
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PrayerCelestialIcon(
                        prayerType = PrayerType.FAJR,
                        size = 34.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Hayya",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Simple, offline prayer times & dependable alarms",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 1. City Selection (Larkana Division, Pakistan)
                OnboardingLocationCard(
                    locationName = locationName,
                    onClick = { showCityPicker = true }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Asr Time Preference
                OnboardingMadhabCard(
                    selectedMadhab = selectedMadhab,
                    onSelectMadhab = onSelectMadhab
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Alarm Permissions
                OnboardingPermissionsCard(
                    isNotificationGranted = isNotificationGranted,
                    isBatteryOptimizationGranted = isBatteryOptimizationGranted,
                    isExactAlarmGranted = isExactAlarmGranted,
                    isFullScreenIntentGranted = isFullScreenIntentGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestBatteryOptimization = onRequestBatteryOptimization,
                    onRequestExactAlarm = onRequestExactAlarm,
                    onRequestFullScreenIntent = onRequestFullScreenIntent
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp)
            ) {
                Button(
                    onClick = onCompleteSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerBottomSheet(
            currentLocationName = locationName,
            onSelectCity = onSelectCity,
            onDismiss = { showCityPicker = false }
        )
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "1. Your Town (Larkana Division)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = locationName.ifEmpty { "Tap to pick your town in Larkana Division" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Choose city",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "2. Afternoon (Asr) Time",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Choose what your local mosque follows",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MadhabChoiceTile(
                    title = "Standard",
                    subtitle = "Earlier Asr",
                    isSelected = selectedMadhab == MadhabOption.STANDARD,
                    onClick = { onSelectMadhab(MadhabOption.STANDARD) },
                    modifier = Modifier.weight(1f)
                )
                MadhabChoiceTile(
                    title = "Hanafi",
                    subtitle = "Later Asr",
                    isSelected = selectedMadhab == MadhabOption.HANAFI,
                    onClick = { onSelectMadhab(MadhabOption.HANAFI) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MadhabChoiceTile(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun OnboardingPermissionsCard(
    isNotificationGranted: Boolean,
    isBatteryOptimizationGranted: Boolean,
    isExactAlarmGranted: Boolean,
    isFullScreenIntentGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestFullScreenIntent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "3. Keep Alarms On Time",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionRowItem(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Notifications",
                    subtitle = if (isNotificationGranted) "Ready — prayer alerts can display" else "Tap to allow prayer alerts",
                    isGranted = isNotificationGranted,
                    onClick = onRequestNotificationPermission
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            PermissionRowItem(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "Unrestricted Battery",
                subtitle = if (isBatteryOptimizationGranted) "Ready — won't sleep in background" else "Tap to allow background alarms",
                isGranted = isBatteryOptimizationGranted,
                onClick = onRequestBatteryOptimization
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionRowItem(
                icon = Icons.Outlined.Alarm,
                title = "Exact Alarm Timing",
                subtitle = if (isExactAlarmGranted) "Ready — rings at the exact minute" else "Tap to allow exact alarm schedule",
                isGranted = isExactAlarmGranted,
                onClick = onRequestExactAlarm
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Spacer(modifier = Modifier.height(12.dp))

                PermissionRowItem(
                    icon = Icons.Outlined.ScreenLockPortrait,
                    title = "Lockscreen Wake",
                    subtitle = if (isFullScreenIntentGranted) "Ready — shows alarm on locked screen" else "Tap to allow full-screen alarm wake",
                    isGranted = isFullScreenIntentGranted,
                    onClick = onRequestFullScreenIntent
                )
            }
        }
    }
}

@Composable
private fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
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
                tint = if (isGranted) HayyaSuccess else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isGranted) HayyaSuccess.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Allowed",
                    tint = HayyaSuccess,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Allow",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
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
            locationName = "Larkana, Pakistan",
            selectedMadhab = MadhabOption.HANAFI,
            isBatteryOptimizationGranted = true,
            isExactAlarmGranted = true,
            isNotificationGranted = true,
            onSelectCity = {},
            onSelectMadhab = {},
            onRequestBatteryOptimization = {},
            onRequestExactAlarm = {},
            onCompleteSetup = {}
        )
    }
}
