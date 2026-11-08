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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.FridayAutoMuteDuration
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.domain.model.VolumeEscalationOption
import dev.hayya.core.system.audio.AutoMuteManager
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TabularCountdownStyle
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
        subtitle = "Pure OLED Dark, Warm Sand Light, Material You dynamic color & Sora typography"
    ),
    CALCULATION(
        title = "CALCULATION & JURISTICS",
        subtitle = "Conventions (13 authorities), Asr Madhab, High Latitude rules & Isha cutoff"
    ),
    ALERTS(
        title = "ALERTS & OFFSETS",
        subtitle = "Auto-silence duration, 3-tier notification toggles & custom minute offsets"
    ),
    RELIABILITY(
        title = "SYSTEM RELIABILITY",
        subtitle = "Pinned foreground notification, battery optimization exemption & OEM guide"
    ),
    SIMULATOR(
        title = "ALARM SIMULATION",
        subtitle = "Simulate real lockscreen wake alarm with countdown timer (5s, 15s, 30s, 60s)"
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
    onSelectCalculationMethod: (CalculationMethodOption) -> Unit,
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
    onUpdateLocation: (Double, Double, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var activeSubSection by remember { mutableStateOf<SettingsSubSection?>(null) }

    // When inside a sub-screen, pressing hardware or gesture back returns to the main Settings menu
    BackHandler(enabled = activeSubSection != null) {
        activeSubSection = null
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val section = activeSubSection) {
            null -> {
                MainSettingsMenu(
                    onBackClick = onBackClick,
                    onOpenSection = { activeSubSection = it }
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
                    onSelectMethod = onSelectCalculationMethod,
                    onSelectMadhab = onSelectMadhab,
                    onSelectHighLatitudeRule = onSelectHighLatitudeRule,
                    onToggleIshaMidnight = onToggleIshaMidnight,
                    onUpdateLocation = onUpdateLocation
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

/**
 * Root Settings Menu: 4 clean, compact button cards navigating to dedicated sub-screens.
 */
@Composable
private fun MainSettingsMenu(
    onBackClick: () -> Unit,
    onOpenSection: (SettingsSubSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Dashboard",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "SETTINGS",
                style = TabularClockStyle.copy(
                    fontSize = 15.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select a category to customize prayer calculation, alerts, and system behavior.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Clean Section Buttons
        SettingsSectionNavButton(
            title = "Appearance & Theming",
            description = "Pure OLED Dark, Warm Light, Material You dynamic color & Sora typography",
            icon = Icons.Outlined.Palette,
            onClick = { onOpenSection(SettingsSubSection.APPEARANCE) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionNavButton(
            title = "Calculation & Juristics",
            description = "Conventions (13 authorities), Asr Madhab, High Latitude rules & Isha cutoff",
            icon = Icons.Outlined.Public,
            onClick = { onOpenSection(SettingsSubSection.CALCULATION) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionNavButton(
            title = "Alerts, Sound & Minute Offsets",
            description = "Auto-silence duration, 3-tier notification toggles & custom minute offsets",
            icon = Icons.Outlined.NotificationsActive,
            onClick = { onOpenSection(SettingsSubSection.ALERTS) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionNavButton(
            title = "Device & System Reliability",
            description = "Pinned foreground notification, battery optimization exemption & OEM guide",
            icon = Icons.Outlined.Shield,
            onClick = { onOpenSection(SettingsSubSection.RELIABILITY) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionNavButton(
            title = "Alarm Test & Simulation",
            description = "Simulate real lockscreen wake alarm with countdown timer (5s, 15s, 30s, 60s)",
            icon = Icons.Outlined.AccessAlarm,
            onClick = { onOpenSection(SettingsSubSection.SIMULATOR) }
        )

        Spacer(modifier = Modifier.height(112.dp))
    }
}

/**
 * Reusable compact navigation card for main settings categories.
 */
@Composable
private fun SettingsSectionNavButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TabularClockStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open section",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Shared container for focused sub-screens.
 */
@Composable
private fun SubScreenContainer(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = TabularClockStyle.copy(
                    fontSize = 14.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        content()

        Spacer(modifier = Modifier.height(112.dp))
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

// ===========================================================================
// SUB-SCREEN 0: APPEARANCE & THEME
// ===========================================================================
@Composable
private fun AppearanceSubScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onSelectThemeMode: (ThemeModeOption) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit
) {
    SubScreenContainer(
        title = "APPEARANCE & THEME",
        onBackClick = onBackClick
    ) {
        // Section: Visual Theme Mode
        SettingsGroupCard(title = "VISUAL THEME") {
            Text(
                text = "Choose your preferred display aesthetic. Pure OLED provides maximum battery savings on OLED panels.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            ThemeModeOption.entries.forEach { mode ->
                val selected = userPreferences.themeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectThemeMode(mode) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mode.subtitle,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Material You Dynamic Color
        SettingsGroupCard(title = "MATERIAL YOU DYNAMIC COLOR") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Color Palette",
                        style = TabularClockStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Extract system wallpaper colors into Material 3 components and UI surfaces."
                        } else {
                            "Requires Android 12 (API 31) or higher. Uses signature Warm Sand on older OS versions."
                        },
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = userPreferences.dynamicColorEnabled,
                    onCheckedChange = { onToggleDynamicColor(it) },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
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

// ===========================================================================
// SUB-SCREEN 1: CALCULATION & JURISTICS
// ===========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationSubScreen(
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onSelectMethod: (CalculationMethodOption) -> Unit,
    onSelectMadhab: (MadhabOption) -> Unit,
    onSelectHighLatitudeRule: (HighLatitudeRuleOption) -> Unit,
    onToggleIshaMidnight: (Boolean) -> Unit,
    onUpdateLocation: (Double, Double, String) -> Unit
) {
    var showMethodPicker by remember { mutableStateOf(false) }
    var showLocationEditor by remember { mutableStateOf(false) }
    var cityInput by remember(userPreferences.location) {
        mutableStateOf(userPreferences.location.resolvedLocationName)
    }
    var latInput by remember(userPreferences.location) {
        mutableStateOf(userPreferences.location.latitude.toString())
    }
    var lngInput by remember(userPreferences.location) {
        mutableStateOf(userPreferences.location.longitude.toString())
    }
    var coordError by remember { mutableStateOf<String?>(null) }

    SubScreenContainer(
        title = "CALCULATION & JURISTICS",
        onBackClick = onBackClick
    ) {
        // Group Card: Astronomical Parameters
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Row 0: Location & Coordinates
                Text(
                    text = "LOCATION & COORDINATES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            cityInput = userPreferences.location.resolvedLocationName
                            latInput = userPreferences.location.latitude.toString()
                            lngInput = userPreferences.location.longitude.toString()
                            coordError = null
                            showLocationEditor = true
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userPreferences.location.resolvedLocationName.ifEmpty { "Makkah, Saudi Arabia" },
                                style = TabularClockStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lat: ${userPreferences.location.latitude}, Lng: ${userPreferences.location.longitude}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Edit",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.height(14.dp))

                // Row 1: Calculation Authority Dropdown
                Text(
                    text = "CALCULATION CONVENTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showMethodPicker = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userPreferences.calculationMethod.displayName,
                                style = TabularClockStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Canonical city: ${userPreferences.calculationMethod.defaultCity}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Change",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.height(14.dp))

                // Row 2: Asr Madhab
                Text(
                    text = "ASR JURISTIC METHOD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shadow length multiplier: Standard (1x) vs Hanafi (2x)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MadhabOption.entries.forEach { option ->
                        val isSelected = option == userPreferences.madhab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectMadhab(option) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (option == MadhabOption.STANDARD) "Standard (1x)" else "Hanafi (2x)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.height(14.dp))

                // Row 3: High Latitude Rule
                Text(
                    text = "HIGH LATITUDE TWILIGHT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Approximation for extreme northern/southern latitudes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HighLatitudeRuleOption.entries.forEach { rule ->
                        val isSelected = rule == userPreferences.highLatitudeRule
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectHighLatitudeRule(rule) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (rule) {
                                    HighLatitudeRuleOption.TWILIGHT_ANGLE -> "Angle"
                                    HighLatitudeRuleOption.SEVENTH_OF_THE_NIGHT -> "1/7th"
                                    HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT -> "Midnight"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.height(14.dp))

                // Row 4: Isha Cutoff Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Isha Window Ends at Midnight",
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (userPreferences.ishaExpiresAtMidnight) {
                                "Window expires at Islamic midnight (halfway to dawn)"
                            } else {
                                "Window continues until next day Fajr"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.ishaExpiresAtMidnight,
                        onCheckedChange = onToggleIshaMidnight,
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

    // Modal Bottom Sheet for Calculation Authority Selection (Clean, Uncluttered Dropdown UX)
    if (showMethodPicker) {
        ModalBottomSheet(
            onDismissRequest = { showMethodPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "SELECT CALCULATION CONVENTION",
                    style = TabularClockStyle.copy(
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose the recognized astronomical authority for your region.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    CalculationMethodOption.entries.forEach { method ->
                        val isSelected = method == userPreferences.calculationMethod
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onSelectMethod(method)
                                    showMethodPicker = false
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = method.displayName,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Canonical region: ${method.defaultCity}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = method.description,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLocationEditor) {
        ModalBottomSheet(
            onDismissRequest = { showLocationEditor = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "SET LOCATION COORDINATES",
                    style = TabularClockStyle.copy(
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("City / Location Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it; coordError = null },
                        label = { Text("Latitude (-90..90)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it; coordError = null },
                        label = { Text("Longitude (-180..180)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (coordError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = coordError!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showLocationEditor = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            val lat = latInput.trim().toDoubleOrNull()
                            val lng = lngInput.trim().toDoubleOrNull()
                            if (lat == null || lat !in -90.0..90.0 || lng == null || lng !in -180.0..180.0) {
                                coordError = "Enter valid latitude (-90..90) and longitude (-180..180)."
                            } else {
                                onUpdateLocation(
                                    lat,
                                    lng,
                                    cityInput.trim().ifEmpty { "Custom Location" }
                                )
                                showLocationEditor = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===========================================================================
// SUB-SCREEN 2: ALERTS & OFFSETS
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
        title = "ALERTS & OFFSETS",
        onBackClick = onBackClick
    ) {
        // Card 1: Audio & Alert Tiers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Auto-Silence Pills
                Text(
                    text = "AUTO-SILENCE AUDIO TIMEOUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Maximum playback time before Adhan audio fades out",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AutoSilenceTimeout.entries.forEach { timeout ->
                        val isSelected = timeout == userPreferences.autoSilenceTimeout
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectAutoSilence(timeout) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (timeout) {
                                    AutoSilenceTimeout.SECONDS_30 -> "30s"
                                    AutoSilenceTimeout.SECONDS_60 -> "1m"
                                    AutoSilenceTimeout.SECONDS_120 -> "2m"
                                    AutoSilenceTimeout.FULL_AUDIO -> "Full"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.height(14.dp))

                // Gradual Volume Escalation Pills
                Text(
                    text = "GRADUAL VOLUME ESCALATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Starts audio at 10% volume and smoothly steps up every 1.5s to 100% to avoid sudden waking at Fajr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectVolumeEscalation(option) }
                                .padding(vertical = 8.dp),
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.height(14.dp))

                // Pre-Reminder Tier 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pre-Reminder (T - 15m)",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Gentle heads-up 15 minutes before prayer time",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.preReminderEnabled,
                        onCheckedChange = onTogglePreReminder,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Adhan Alarm Tier 2 (Locked Active)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Main Adhan Alarm (T0 Start)",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Full lockscreen wake screen & Adhan audio",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ALWAYS ON",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Last Call Tier 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Last Call (Expiry - 20m)",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Urgent alert 20 minutes before window closes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.lastCallEnabled,
                        onCheckedChange = onToggleLastCall,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Post-Adhan Dua on Wake Screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Post-Adhan Dua on Wake Screen",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Display authentic Bukhari 614 Dua on alarm screen",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.showPostAdhanDua,
                        onCheckedChange = onTogglePostAdhanDua,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tahajjud (Qiyam al-Layl) Pre-Alarm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tahajjud Pre-Alarm (Qiyam al-Layl)",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Gentle wake-up alert at the Last Third of the Night",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.tahajjudAlarmEnabled,
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

        Spacer(modifier = Modifier.height(14.dp))

        // Card 1.5: Congregational Prayer Auto-Mute (Mosque Mode)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONGREGATIONAL AUTO-MUTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mosque Silence Mode",
                            style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    if (userPreferences.autoMuteDuration != AutoMuteDuration.OFF) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automatically switches system ringer to Silent or Vibrate at Adhan so your phone never rings in the mosque, then restores your exact previous volume afterward.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Duration Selector
                Text(
                    text = "SILENCE DURATION AFTER ADHAN",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AutoMuteDuration.entries.forEach { duration ->
                        val isSelected = duration == userPreferences.autoMuteDuration
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectAutoMuteDuration(duration) }
                                .padding(vertical = 8.dp),
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Friday (Jumu'ah) Khutbah Extended Auto-Mute Duration Selector
                Text(
                    text = "FRIDAY (JUMU'AH) KHUTBAH EXTENDED AUTO-MUTE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Extended silence duration covering the Friday sermon (Khutbah) and congregational prayer.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FridayAutoMuteDuration.entries.forEach { duration ->
                        val isSelected = duration == userPreferences.fridayAutoMuteDuration
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelectFridayAutoMuteDuration(duration) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (duration) {
                                    FridayAutoMuteDuration.MATCH_STANDARD -> "Standard"
                                    FridayAutoMuteDuration.MINUTES_45 -> "45m"
                                    FridayAutoMuteDuration.MINUTES_60 -> "60m"
                                    FridayAutoMuteDuration.OFF -> "Off"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // If AutoMute is enabled (either standard or Friday), display target mode selector & DND permission check
                val isAutoMuteEnabled = userPreferences.autoMuteDuration != AutoMuteDuration.OFF ||
                    userPreferences.fridayAutoMuteDuration != FridayAutoMuteDuration.OFF
                if (isAutoMuteEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "TARGET RINGER MODE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoMuteTargetMode.entries.forEach { mode ->
                            val isSelected = mode == userPreferences.autoMuteTargetMode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onSelectAutoMuteTargetMode(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    val context = LocalContext.current
                    val hasDndAccess = remember { AutoMuteManager.hasNotificationPolicyAccess(context) }
                    if (userPreferences.autoMuteTargetMode == AutoMuteTargetMode.SILENT && !hasDndAccess) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Do Not Disturb Access Needed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Android requires Do Not Disturb permission to toggle Silent mode. Without it, Hayya will safely fall back to Vibrate mode.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { AutoMuteManager.openNotificationPolicySettings(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Text(
                                        text = "Grant Do Not Disturb Access",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: Manual Minute Adjustments (-30 to +30)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MANUAL MINUTE OFFSETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Calibrate calculations (-30 to +30m) to match your local masjid",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                PrayerType.entries.forEachIndexed { index, prayer ->
                    val currentOffset = userPreferences.manualMinuteOffsets[prayer] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prayer.name,
                            style = TabularClockStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (currentOffset > -30) onUpdateOffset(prayer, currentOffset - 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (currentOffset > 0) "+${currentOffset}m" else "${currentOffset}m",
                                style = TabularClockStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.width(36.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (currentOffset < 30) onUpdateOffset(prayer, currentOffset + 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (index < PrayerType.entries.size - 1) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
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
        title = "SYSTEM RELIABILITY",
        onBackClick = onBackClick
    ) {
        // Group Card: Reliability Suite
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Section 1: Pinned Prayer Status Foreground Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (userPreferences.pinnedNotificationEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = if (userPreferences.pinnedNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pinned Prayer Status",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = if (userPreferences.pinnedNotificationEnabled) "Active • Process anchored in RAM" else "Disabled • Anchors process against LMK",
                            fontSize = 11.sp,
                            color = if (userPreferences.pinnedNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = userPreferences.pinnedNotificationEnabled,
                        onCheckedChange = onTogglePinnedNotification,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Displays an ongoing low-priority status notification. Highly recommended for aggressive OEM task killers (Samsung OneUI, Xiaomi HyperOS, Oppo, Vivo) to guarantee alarms fire on time.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: OEM Battery Optimization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBatteryOptimizationIgnored) Icons.Outlined.Shield else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatteryOptimizationIgnored) "Battery Exemption Active" else "Battery Restriction Detected",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = if (isBatteryOptimizationIgnored) "Alarms protected during Doze mode" else "Action required: choose 'Unrestricted'",
                            fontSize = 11.sp,
                            color = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ensure Hayya is set to 'Unrestricted' in App Info > Battery so Android does not delay Adhan alarms when the device enters deep sleep.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLaunchBatterySettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isBatteryOptimizationIgnored) "Open App Battery Settings" else "Open App Info > Battery Directly",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ===========================================================================
// SUB-SCREEN 4: ALARM SIMULATION
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
        title = "ALARM SIMULATION",
        onBackClick = onBackClick
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessAlarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lockscreen Alarm Test",
                        style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Test the full-screen lockscreen wake, Adhan audio playback, and tactile slide-to-dismiss slider. Pick a countdown, tap trigger, and lock your screen.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Timer Pills
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
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedDelay = seconds }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${seconds}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Firing alarm in ${countdownRemaining}s... Lock your screen now!",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
