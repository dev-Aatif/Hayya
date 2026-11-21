package dev.hayya.core.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.CityPreset
import dev.hayya.core.domain.model.FridayAutoMuteDuration
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.domain.model.VolumeEscalationOption
import dev.hayya.core.system.audio.AutoMuteManager
import dev.hayya.core.ui.components.CityPickerBottomSheet
import dev.hayya.core.ui.components.SectionPageHeader
import dev.hayya.core.ui.components.friendlyPrayerName
import dev.hayya.core.ui.theme.HayyaSuccess
import dev.hayya.core.ui.theme.TabularClockStyle
import kotlinx.coroutines.delay

/**
 * Settings categories, each displayed on its own focused sub-screen.
 */
enum class SettingsSubSection(
    val title: String,
    val subtitle: String
) {
    APPEARANCE(
        title = "APPEARANCE & THEME",
        subtitle = "Dark OLED, Warm Light, or System appearance"
    ),
    CALCULATION(
        title = "CALCULATION & JURISTICS",
        subtitle = "City, prayer time method, and Asr setting"
    ),
    ALERTS(
        title = "ALERTS & OFFSETS",
        subtitle = "Adhan sound, Mosque silence, and time tweaks"
    ),
    RELIABILITY(
        title = "SYSTEM RELIABILITY",
        subtitle = "Keep alarms working in the background"
    ),
    SIMULATOR(
        title = "ALARM SIMULATION",
        subtitle = "Try the lockscreen alarm and sound"
    )
}

@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    isBatteryOptimizationIgnored: Boolean,
    onBackClick: () -> Unit,
    onSelectThemeMode: (ThemeModeOption) -> Unit = {},
    onToggleDynamicColor: (Boolean) -> Unit = {},
    onSelectAutoSilence: (AutoSilenceTimeout) -> Unit,
    onSelectAutoMuteDuration: (AutoMuteDuration) -> Unit = {},
    onSelectFridayAutoMuteDuration: (FridayAutoMuteDuration) -> Unit = {},
    onSelectAutoMuteTargetMode: (AutoMuteTargetMode) -> Unit = {},
    onSelectMadhab: (MadhabOption) -> Unit,
    onToggleIshaMidnight: (Boolean) -> Unit,
    onTogglePreReminder: (Boolean) -> Unit,
    onToggleLastCall: (Boolean) -> Unit,
    onTogglePinnedNotification: (Boolean) -> Unit,
    onTogglePostAdhanDua: (Boolean) -> Unit = {},
    onSelectVolumeEscalation: (VolumeEscalationOption) -> Unit = {},
    onToggleTahajjudAlarm: (Boolean) -> Unit = {},
    onSelectHighLatitudeRule: (HighLatitudeRuleOption) -> Unit,
    onUpdatePrayerOffset: (PrayerType, Int) -> Unit,
    onLaunchBatterySettings: () -> Unit,
    onTriggerTestAlarm: (Int) -> Unit,
    onSelectCity: (CityPreset) -> Unit = {},
    onOpenSetupGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubSection by remember { mutableStateOf<SettingsSubSection?>(null) }

    BackHandler(enabled = activeSubSection != null) {
        activeSubSection = null
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (activeSubSection) {
            null -> {
                MainSettingsMenu(
                    userPreferences = userPreferences,
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    onOpenSection = { activeSubSection = it },
                    onOpenSetupGuide = onOpenSetupGuide
                )
            }
            SettingsSubSection.APPEARANCE -> {
                AppearanceSubScreen(
                    userPreferences = userPreferences,
                    onBackClick = { activeSubSection = null },
                    onSelectThemeMode = onSelectThemeMode,
                    onToggleDynamicColor = onToggleDynamicColor
                )
            }
            SettingsSubSection.CALCULATION -> {
                CalculationSubScreen(
                    userPreferences = userPreferences,
                    onBackClick = { activeSubSection = null },
                    onSelectMadhab = onSelectMadhab,
                    onSelectHighLatitudeRule = onSelectHighLatitudeRule,
                    onToggleIshaMidnight = onToggleIshaMidnight,
                    onSelectCity = onSelectCity
                )
            }
            SettingsSubSection.ALERTS -> {
                AlertsSubScreen(
                    userPreferences = userPreferences,
                    onBackClick = { activeSubSection = null },
                    onSelectAutoSilence = onSelectAutoSilence,
                    onSelectAutoMuteDuration = onSelectAutoMuteDuration,
                    onSelectFridayAutoMuteDuration = onSelectFridayAutoMuteDuration,
                    onSelectAutoMuteTargetMode = onSelectAutoMuteTargetMode,
                    onSelectVolumeEscalation = onSelectVolumeEscalation,
                    onToggleTahajjudAlarm = onToggleTahajjudAlarm,
                    onTogglePreReminder = onTogglePreReminder,
                    onToggleLastCall = onToggleLastCall,
                    onTogglePostAdhanDua = onTogglePostAdhanDua,
                    onUpdateOffset = onUpdatePrayerOffset
                )
            }
            SettingsSubSection.RELIABILITY -> {
                ReliabilitySubScreen(
                    userPreferences = userPreferences,
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    onBackClick = { activeSubSection = null },
                    onTogglePinnedNotification = onTogglePinnedNotification,
                    onLaunchBatterySettings = onLaunchBatterySettings
                )
            }
            SettingsSubSection.SIMULATOR -> {
                SimulatorSubScreen(
                    onBackClick = { activeSubSection = null },
                    onTriggerTestAlarm = onTriggerTestAlarm
                )
            }
        }
    }
}

@Composable
private fun MainSettingsMenu(
    userPreferences: UserPreferences,
    isBatteryOptimizationIgnored: Boolean,
    onOpenSection: (SettingsSubSection) -> Unit,
    onOpenSetupGuide: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Customize your location, sound, and look",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Visual Status Summary Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onOpenSection(SettingsSubSection.RELIABILITY) },
            shape = RoundedCornerShape(20.dp),
            color = if (isBatteryOptimizationIgnored) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            },
            border = BorderStroke(
                1.dp,
                if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBatteryOptimizationIgnored) HayyaSuccess.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBatteryOptimizationIgnored) Icons.Outlined.Shield else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (isBatteryOptimizationIgnored) HayyaSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isBatteryOptimizationIgnored) "Alarms Protected" else "Battery Check Needed",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "${userPreferences.location.resolvedLocationName.substringBefore(",")} • ${userPreferences.themeMode.displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Unified Clean Directory
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSectionNavRow(
                    title = "Appearance",
                    description = "Dark OLED, Warm Light, or System theme",
                    icon = Icons.Outlined.Palette,
                    onClick = { onOpenSection(SettingsSubSection.APPEARANCE) }
                )

                SettingsNavDivider()

                SettingsSectionNavRow(
                    title = "Location & Prayer Times",
                    description = "City, calculation method, and Asr time",
                    icon = Icons.Outlined.Public,
                    onClick = { onOpenSection(SettingsSubSection.CALCULATION) }
                )

                SettingsNavDivider()

                SettingsSectionNavRow(
                    title = "Alarms & Mosque Mode",
                    description = "Alarm duration, fade-in, auto-mute, and offsets",
                    icon = Icons.Outlined.NotificationsActive,
                    onClick = { onOpenSection(SettingsSubSection.ALERTS) }
                )

                SettingsNavDivider()

                SettingsSectionNavRow(
                    title = "Alarm Reliability",
                    description = "Keep Hayya active and safe from battery sleep",
                    icon = Icons.Outlined.Shield,
                    onClick = { onOpenSection(SettingsSubSection.RELIABILITY) }
                )

                SettingsNavDivider()

                SettingsSectionNavRow(
                    title = "Test Your Alarm",
                    description = "Preview the alarm tone and lockscreen wake",
                    icon = Icons.Outlined.AccessAlarm,
                    onClick = { onOpenSection(SettingsSubSection.SIMULATOR) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Setup Wizard Re-entry
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenSetupGuide),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Quick Setup Guide",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Re-run the 3-step welcome setup",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun SettingsNavDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
    )
}

@Composable
private fun SettingsSectionNavRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Open $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SubScreenContainer(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionPageHeader(
            title = title,
            subtitle = subtitle,
            onBackClick = onBackClick
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// ===========================================================================
// SUB-SCREEN 0: APPEARANCE
// ===========================================================================
@Composable
private fun AppearanceSubScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onSelectThemeMode: (ThemeModeOption) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit
) {
    SubScreenContainer(
        title = "Appearance",
        subtitle = "Theme & visual style",
        onBackClick = onBackClick
    ) {
        SettingsGroupCard(title = "Choose Theme") {
            // Visual 3-card theme preview row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeModeOption.entries.forEach { mode ->
                    val selected = userPreferences.themeMode == mode
                    val (previewBg, previewCard, previewAccent) = when (mode) {
                        ThemeModeOption.DARK -> Triple(Color(0xFF07080A), Color(0xFF15181E), Color(0xFFE7C476))
                        ThemeModeOption.LIGHT -> Triple(Color(0xFFF8F4EC), Color(0xFFFFFFFF), Color(0xFF8C6200))
                        ThemeModeOption.SYSTEM -> Triple(Color(0xFF1E2229), Color(0xFFF3ECE0), Color(0xFFE7C476))
                    }
                    val shortTitle = when (mode) {
                        ThemeModeOption.DARK -> "Dark"
                        ThemeModeOption.LIGHT -> "Light"
                        ThemeModeOption.SYSTEM -> "Auto"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectThemeMode(mode) }
                            .padding(10.dp)
                    ) {
                        // Mini phone UI thumbnail
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(previewBg)
                                .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .padding(7.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(previewAccent)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(previewCard)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(previewCard.copy(alpha = 0.7f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = shortTitle,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsGroupCard(title = "Wallpaper Colors") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Match System Wallpaper",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Tint buttons and highlights using your phone's wallpaper."
                        } else {
                            "Available on Android 12 and newer."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = userPreferences.dynamicColorEnabled,
                    onCheckedChange = onToggleDynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

// ===========================================================================
// SUB-SCREEN 1: LOCATION & PRAYER TIMES
// ===========================================================================
@Composable
private fun CalculationSubScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onSelectMadhab: (MadhabOption) -> Unit,
    onSelectHighLatitudeRule: (HighLatitudeRuleOption) -> Unit,
    onToggleIshaMidnight: (Boolean) -> Unit,
    onSelectCity: (CityPreset) -> Unit
) {
    var showCityPicker by remember { mutableStateOf(false) }

    SubScreenContainer(
        title = "Location & Times",
        subtitle = "City, method & Asr school",
        onBackClick = onBackClick
    ) {
        SettingsGroupCard(title = "City") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showCityPicker = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = userPreferences.location.resolvedLocationName.ifEmpty { "Larkana, Pakistan" },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Tap to search towns in Larkana Division",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsGroupCard(title = "Prayer Time Authority") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = CalculationMethodOption.KARACHI.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pakistan Sunni Standard • Fajr 18.0° / Isha 18.0°",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Pakistan",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Afternoon (Asr) Time",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MadhabOption.entries.forEach { option ->
                    val isSelected = option == userPreferences.madhab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMadhab(option) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (option == MadhabOption.STANDARD) "Standard" else "Hanafi",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (option == MadhabOption.STANDARD) "Earlier Asr" else "Later Asr",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Far North / South Summer Rule",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HighLatitudeRuleOption.entries.forEach { rule ->
                    val isSelected = rule == userPreferences.highLatitudeRule
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { onSelectHighLatitudeRule(rule) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (rule) {
                                HighLatitudeRuleOption.TWILIGHT_ANGLE -> "Angle"
                                HighLatitudeRuleOption.SEVENTH_OF_THE_NIGHT -> "1/7th"
                                HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT -> "Midnight"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Isha Ends at Midnight",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (userPreferences.ishaExpiresAtMidnight) {
                            "Isha ends halfway through the night"
                        } else {
                            "Isha continues until Fajr dawn"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Switch(
                    checked = userPreferences.ishaExpiresAtMidnight,
                    onCheckedChange = onToggleIshaMidnight,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }

    if (showCityPicker) {
        CityPickerBottomSheet(
            currentLocationName = userPreferences.location.resolvedLocationName,
            onSelectCity = onSelectCity,
            onDismiss = { showCityPicker = false }
        )
    }
}

// ===========================================================================
// SUB-SCREEN 2: ALARMS & MOSQUE MODE
// ===========================================================================
@Composable
private fun AlertsSubScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onSelectAutoSilence: (AutoSilenceTimeout) -> Unit,
    onSelectAutoMuteDuration: (AutoMuteDuration) -> Unit,
    onSelectFridayAutoMuteDuration: (FridayAutoMuteDuration) -> Unit = {},
    onSelectAutoMuteTargetMode: (AutoMuteTargetMode) -> Unit,
    onSelectVolumeEscalation: (VolumeEscalationOption) -> Unit,
    onToggleTahajjudAlarm: (Boolean) -> Unit,
    onTogglePreReminder: (Boolean) -> Unit,
    onToggleLastCall: (Boolean) -> Unit,
    onTogglePostAdhanDua: (Boolean) -> Unit,
    onUpdateOffset: (PrayerType, Int) -> Unit
) {
    SubScreenContainer(
        title = "Alarms & Sound",
        subtitle = "Alarm duration, reminders & Mosque mode",
        onBackClick = onBackClick
    ) {
        SettingsGroupCard(title = "Alarm Sound (Default Device Tone)") {
            Text(
                text = "Uses your device's default alarm tone • Stop automatically after",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutoSilenceTimeout.entries.forEach { timeout ->
                    val isSelected = timeout == userPreferences.autoSilenceTimeout
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { onSelectAutoSilence(timeout) }
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
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gentle Wake Fade-In",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = "Starts soft and slowly increases volume",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VolumeEscalationOption.entries.forEach { option ->
                    val isSelected = option == userPreferences.volumeEscalation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { onSelectVolumeEscalation(option) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (option) {
                                VolumeEscalationOption.OFF -> "Off"
                                VolumeEscalationOption.SECONDS_10 -> "10s"
                                VolumeEscalationOption.SECONDS_15 -> "15s"
                                VolumeEscalationOption.SECONDS_20 -> "20s"
                                VolumeEscalationOption.SECONDS_30 -> "30s"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            AlertSwitchRow(
                title = "15-Minute Early Reminder",
                subtitle = "Quiet notification before prayer starts",
                checked = userPreferences.preReminderEnabled,
                onCheckedChange = onTogglePreReminder
            )
            Spacer(modifier = Modifier.height(12.dp))
            AlertSwitchRow(
                title = "Last Call Warning",
                subtitle = "Reminder 20 minutes before prayer ends",
                checked = userPreferences.lastCallEnabled,
                onCheckedChange = onToggleLastCall
            )
            Spacer(modifier = Modifier.height(12.dp))
            AlertSwitchRow(
                title = "Show Dua on Wake Screen",
                subtitle = "Button to read the Post-Adhan Dua",
                checked = userPreferences.showPostAdhanDua,
                onCheckedChange = onTogglePostAdhanDua
            )
            Spacer(modifier = Modifier.height(12.dp))
            AlertSwitchRow(
                title = "Tahajjud Wake Alert",
                subtitle = "Gentle alert at the Last Third of the Night",
                checked = userPreferences.tahajjudAlarmEnabled,
                onCheckedChange = onToggleTahajjudAlarm
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mosque Silence Card
        SettingsGroupCard(title = "Mosque Auto-Silent") {
            Text(
                text = "Automatically silences your phone at prayer time and turns your ringtone back on afterward.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Daily Prayer Silence",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AutoMuteDuration.entries.forEach { duration ->
                    val isSelected = duration == userPreferences.autoMuteDuration
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { onSelectAutoMuteDuration(duration) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (duration) {
                                AutoMuteDuration.OFF -> "Off"
                                AutoMuteDuration.MINUTES_15 -> "15m"
                                AutoMuteDuration.MINUTES_20 -> "20m"
                                AutoMuteDuration.MINUTES_25 -> "25m"
                                AutoMuteDuration.MINUTES_30 -> "30m"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Friday Khutbah Silence",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FridayAutoMuteDuration.entries.forEach { duration ->
                    val isSelected = duration == userPreferences.fridayAutoMuteDuration
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { onSelectFridayAutoMuteDuration(duration) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (duration) {
                                FridayAutoMuteDuration.MATCH_STANDARD -> "Same"
                                FridayAutoMuteDuration.MINUTES_45 -> "45m"
                                FridayAutoMuteDuration.MINUTES_60 -> "60m"
                                FridayAutoMuteDuration.OFF -> "Off"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            val isAutoMuteEnabled = userPreferences.autoMuteDuration != AutoMuteDuration.OFF ||
                userPreferences.fridayAutoMuteDuration != FridayAutoMuteDuration.OFF
            if (isAutoMuteEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Switch Phone To",
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AutoMuteTargetMode.entries.forEach { mode ->
                        val isSelected = mode == userPreferences.autoMuteTargetMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .clickable { onSelectAutoMuteTargetMode(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                var hasDndAccess by remember {
                    mutableStateOf(AutoMuteManager.hasNotificationPolicyAccess(context))
                }
                DisposableEffect(lifecycleOwner, context) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasDndAccess = AutoMuteManager.hasNotificationPolicyAccess(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                if (userPreferences.autoMuteTargetMode == AutoMuteTargetMode.SILENT && !hasDndAccess) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Do Not Disturb Permission Needed",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Allow Do Not Disturb access so Hayya can silence your phone automatically.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { AutoMuteManager.openNotificationPolicySettings(context) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text(
                                    text = "Allow Do Not Disturb",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Minute Adjustments
        SettingsGroupCard(title = "Fine-Tune Prayer Times") {
            Text(
                text = "Shift any time by a few minutes to match your local mosque.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            PrayerType.entries.forEach { prayer ->
                val currentOffset = userPreferences.manualMinuteOffsets[prayer] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friendlyPrayerName(prayer),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (currentOffset > -30) onUpdateOffset(prayer, currentOffset - 1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = if (currentOffset > 0) "+${currentOffset}m" else "${currentOffset}m",
                            style = TabularClockStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(44.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    if (currentOffset < 30) onUpdateOffset(prayer, currentOffset + 1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// ===========================================================================
// SUB-SCREEN 3: SYSTEM RELIABILITY
// ===========================================================================
@Composable
private fun ReliabilitySubScreen(
    userPreferences: UserPreferences,
    isBatteryOptimizationIgnored: Boolean,
    onBackClick: () -> Unit,
    onTogglePinnedNotification: (Boolean) -> Unit,
    onLaunchBatterySettings: () -> Unit
) {
    SubScreenContainer(
        title = "Alarm Reliability",
        subtitle = "Keep alarms working while phone sleeps",
        onBackClick = onBackClick
    ) {
        SettingsGroupCard(title = "Battery Protection") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBatteryOptimizationIgnored) HayyaSuccess.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBatteryOptimizationIgnored) Icons.Outlined.Shield else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (isBatteryOptimizationIgnored) HayyaSuccess else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBatteryOptimizationIgnored) "Battery Unrestricted ✓" else "Battery Restriction Active",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isBatteryOptimizationIgnored) {
                            "Alarms can wake your device anytime."
                        } else {
                            "Set Battery to 'Unrestricted' so alarms aren't delayed."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isBatteryOptimizationIgnored) HayyaSuccess else MaterialTheme.colorScheme.error
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onLaunchBatterySettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isBatteryOptimizationIgnored) "View Battery Settings" else "Fix Battery Setting",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsGroupCard(title = "Background Shield") {
            AlertSwitchRow(
                title = "Keep Active in Notification Shade",
                subtitle = "Shows next prayer countdown in your status bar so your phone won't close Hayya.",
                checked = userPreferences.pinnedNotificationEnabled,
                onCheckedChange = onTogglePinnedNotification
            )
        }
    }
}

// ===========================================================================
// SUB-SCREEN 4: TEST ALARM
// ===========================================================================
@Composable
private fun SimulatorSubScreen(
    onBackClick: () -> Unit,
    onTriggerTestAlarm: (Int) -> Unit
) {
    var selectedDelay by remember { mutableIntStateOf(15) }
    var countdownRemaining by remember { mutableIntStateOf(-1) }

    LaunchedEffect(countdownRemaining) {
        if (countdownRemaining > 0) {
            delay(1000L)
            countdownRemaining -= 1
        }
    }

    SubScreenContainer(
        title = "Test Your Alarm",
        subtitle = "Preview sound & lockscreen wake",
        onBackClick = onBackClick
    ) {
        SettingsGroupCard(title = "How to Test") {
            Text(
                text = "1. Pick a delay below.\n2. Tap Start Test.\n3. Lock your phone screen to see Hayya wake up with your alarm tone.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 15, 30, 60).forEach { seconds ->
                    val isSelected = selectedDelay == seconds
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { selectedDelay = seconds }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${seconds}s",
                            style = TabularClockStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (countdownRemaining > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ringing in ${countdownRemaining}s — lock your screen now!",
                        style = TabularClockStyle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            } else {
                Button(
                    onClick = {
                        countdownRemaining = selectedDelay
                        onTriggerTestAlarm(selectedDelay)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start Test Alarm (${selectedDelay}s)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
