package dev.hayya.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.hayya.core.HayyaApplication
import dev.hayya.core.system.device.OemBatteryHelper
import dev.hayya.core.system.notifications.PinnedPrayerStatusService
import dev.hayya.core.system.oem.OemPowerManager
import dev.hayya.core.ui.components.AppNavTab
import dev.hayya.core.ui.components.FloatingNavBar
import dev.hayya.core.ui.screens.MainDashboardScreen
import dev.hayya.core.ui.screens.OnboardingScreen
import dev.hayya.core.ui.screens.QiblaScreen
import dev.hayya.core.ui.screens.RamadanScreen
import dev.hayya.core.ui.screens.SettingsScreen
import dev.hayya.core.ui.screens.TasbihScreen
import dev.hayya.core.ui.screens.ToolsScreen
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.viewmodel.PrayerViewModel
import dev.hayya.core.widget.PrayerWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Main application entry activity hosting the dashboard, settings, and onboarding flows.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkPermissionsState()
        }
    }

    private val viewModel: PrayerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as? HayyaApplication ?: HayyaApplication.instance
                return PrayerViewModel(
                    calculationEngine = app.calculationEngine,
                    preferencesRepository = app.preferencesRepository,
                    alarmScheduler = app.alarmScheduler,
                    prayerLogRepository = app.prayerLogRepository,
                    fastTrackerRepository = app.fastTrackerRepository
                ) as T
            }
        }
    }

    // Reactive state for missing permissions dialog
    private val missingPermissionsState = mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as? HayyaApplication ?: HayyaApplication.instance

        // Initial permissions check & rolling alarm sync
        checkPermissionsState()

        lifecycleScope.launch {
            app.alarmScheduler.scheduleRollingAlarms()
            PrayerWidgetUpdater.updateAllWidgets(this@MainActivity)
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode = state.userPreferences.themeMode
            val dynamicColor = state.userPreferences.dynamicColorEnabled

            HayyaTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isBatteryIgnored by remember {
                        mutableStateOf(OemBatteryHelper.isIgnoringBatteryOptimizations(this@MainActivity))
                    }
                    var isExactAlarmGranted by remember {
                        mutableStateOf(app.alarmScheduler.canScheduleExactAlarms())
                    }
                    var showExitDialog by remember { mutableStateOf(false) }
                    val missingPermissions by missingPermissionsState

                    LaunchedEffect(Unit) {
                        isBatteryIgnored = OemPowerManager.isBatteryOptimizationIgnored(this@MainActivity)
                        isExactAlarmGranted = app.alarmScheduler.canScheduleExactAlarms()
                        checkPermissionsState()
                    }

                    LaunchedEffect(state.userPreferences.pinnedNotificationEnabled) {
                        if (state.userPreferences.pinnedNotificationEnabled) {
                            PinnedPrayerStatusService.start(this@MainActivity)
                        } else {
                            PinnedPrayerStatusService.stop(this@MainActivity)
                        }
                    }

                    // Back Button in Ramadan navigates back to Companion or Dashboard
                    BackHandler(enabled = state.isRamadanOpen) {
                        viewModel.setRamadanOpen(false)
                    }

                    // Back Button in Tasbih navigates back to Tools
                    BackHandler(enabled = state.isTasbihOpen) {
                        viewModel.setTasbihOpen(false)
                    }

                    // Back Button in Qibla navigates back to Tools
                    BackHandler(enabled = state.isQiblaOpen) {
                        viewModel.setQiblaOpen(false)
                    }

                    // Back Button on Tools or Settings navigates back to Dashboard
                    BackHandler(enabled = !state.isRamadanOpen && !state.isTasbihOpen && !state.isQiblaOpen && state.currentTab != AppNavTab.DASHBOARD) {
                        viewModel.selectNavTab(AppNavTab.DASHBOARD)
                    }

                    // Back Button on Dashboard asks first before closing
                    BackHandler(enabled = !state.isRamadanOpen && !state.isTasbihOpen && !state.isQiblaOpen && state.currentTab == AppNavTab.DASHBOARD) {
                        showExitDialog = true
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            state.isRamadanOpen -> {
                                RamadanScreen(
                                    state = state,
                                    calculationEngine = app.calculationEngine,
                                    onBackClick = { viewModel.setRamadanOpen(false) },
                                    onToggleTodayFast = { viewModel.toggleTodayFast() },
                                    onSetFastStatus = { date, status -> viewModel.setFastStatus(date, status) },
                                    onCycleFastStatus = { date -> viewModel.cycleFastStatus(date) },
                                    onUpdateQadhaMadeUp = { viewModel.updateQadhaMadeUp(it) },
                                    onUpdateSuhoorAlarm = { enabled, mins -> viewModel.updateSuhoorAlarm(enabled, mins) },
                                    onUpdateIftarAlert = { viewModel.updateIftarAlert(it) },
                                    onUpdateEidAlert = { viewModel.updateEidAlert(it) }
                                )
                            }
                            state.isTasbihOpen -> {
                                TasbihScreen(
                                    count = state.tasbihCount,
                                    targetMode = state.tasbihMode,
                                    onIncrement = { viewModel.incrementTasbih() },
                                    onReset = { viewModel.resetTasbih() },
                                    onSelectMode = { viewModel.setTasbihMode(it) },
                                    onBackClick = { viewModel.setTasbihOpen(false) }
                                )
                            }
                            state.isQiblaOpen -> {
                                QiblaScreen(
                                    locationName = state.locationName,
                                    latitude = state.userPreferences.location.latitude,
                                    longitude = state.userPreferences.location.longitude,
                                    onBackClick = { viewModel.setQiblaOpen(false) }
                                )
                            }
                            else -> {
                                when (state.currentTab) {
                                    AppNavTab.DASHBOARD -> {
                                        MainDashboardScreen(
                                            state = state,
                                            onOpenSettings = { viewModel.selectNavTab(AppNavTab.SETTINGS) },
                                            onOpenTasbih = { viewModel.setTasbihOpen(true) },
                                            onOpenQibla = { viewModel.setQiblaOpen(true) },
                                            onOpenRamadan = { viewModel.setRamadanOpen(true) },
                                            onToggleAudioMode = { viewModel.togglePrayerAudioMode(it) },
                                            onTogglePrayerCompletion = { viewModel.togglePrayerCompletion(it) },
                                            onToggleNightFastingCard = { viewModel.toggleNightFastingCardExpanded() },
                                            onToggleTahajjudAlarm = { viewModel.toggleTahajjudAlarm(it) },
                                            onToggleSurahKahfRead = { viewModel.toggleSurahKahfRead() },
                                            onActivateKhutbahMute = { viewModel.activateKhutbahAutoMute(this@MainActivity) },
                                            onRestoreRingerMode = { viewModel.restoreNormalRingerMode(this@MainActivity) }
                                        )
                                    }
                                    AppNavTab.TOOLS -> {
                                        ToolsScreen(
                                            state = state,
                                            onOpenCompass = { viewModel.setQiblaOpen(true) },
                                            onOpenTasbih = { viewModel.setTasbihOpen(true) },
                                            onOpenRamadan = { viewModel.setRamadanOpen(true) },
                                            onIncrementTasbih = { viewModel.incrementTasbih() },
                                            onTogglePrayerCompletion = { viewModel.togglePrayerCompletion(it) },
                                            onTogglePostAdhanDua = { viewModel.togglePostAdhanDua(it) },
                                            onToggleTahajjudAlarm = { viewModel.toggleTahajjudAlarm(it) },
                                            onLaunchBatterySettings = {
                                                OemPowerManager.openBatteryUsageSettings(this@MainActivity)
                                                isBatteryIgnored = OemPowerManager.isBatteryOptimizationIgnored(this@MainActivity)
                                            },
                                            onTriggerTestAlarm = { delaySeconds ->
                                                viewModel.scheduleTestAlarm(delaySeconds)
                                            }
                                        )
                                    }
                                    AppNavTab.SETTINGS -> {
                                        SettingsScreen(
                                            userPreferences = state.userPreferences,
                                            isBatteryOptimizationIgnored = isBatteryIgnored,
                                            onBackClick = { viewModel.selectNavTab(AppNavTab.DASHBOARD) },
                                            onSelectThemeMode = { viewModel.updateThemeMode(it) },
                                            onToggleDynamicColor = { viewModel.updateDynamicColorEnabled(it) },
                                            onSelectAutoSilence = { viewModel.updateAutoSilence(it) },
                                            onSelectAutoMuteDuration = { viewModel.updateAutoMuteDuration(it) },
                                            onSelectFridayAutoMuteDuration = { viewModel.updateFridayAutoMuteDuration(it) },
                                            onSelectAutoMuteTargetMode = { viewModel.updateAutoMuteTargetMode(it) },
                                            onSelectMadhab = { viewModel.updateJuristicMethod(it) },
                                            onSelectCalculationMethod = { viewModel.updateCalculationMethod(it) },
                                            onToggleIshaMidnight = { viewModel.updateIshaExpiresAtMidnight(it) },
                                            onTogglePreReminder = { viewModel.togglePreReminder(it) },
                                            onToggleLastCall = { viewModel.toggleLastCall(it) },
                                            onTogglePinnedNotification = { viewModel.togglePinnedNotification(it) },
                                            onTogglePostAdhanDua = { viewModel.togglePostAdhanDua(it) },
                                            onSelectVolumeEscalation = { viewModel.updateVolumeEscalation(it) },
                                            onToggleTahajjudAlarm = { viewModel.toggleTahajjudAlarm(it) },
                                            onSelectHighLatitudeRule = { viewModel.updateHighLatitudeRule(it) },
                                            onUpdatePrayerOffset = { prayer, offset ->
                                                viewModel.updatePrayerOffset(prayer, offset)
                                            },
                                            onLaunchBatterySettings = {
                                                OemPowerManager.openBatteryUsageSettings(this@MainActivity)
                                                isBatteryIgnored = OemPowerManager.isBatteryOptimizationIgnored(this@MainActivity)
                                            },
                                            onTriggerTestAlarm = { delaySeconds ->
                                                viewModel.scheduleTestAlarm(delaySeconds)
                                            },
                                            onUpdateLocation = { lat, lng, name ->
                                                viewModel.updateLocation(lat, lng, name)
                                            }
                                        )
                                    }
                                }

                                // Modern Floating Navbar docked at bottom center
                                FloatingNavBar(
                                    currentTab = state.currentTab,
                                    onTabSelected = { viewModel.selectNavTab(it) },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }

                    // Exit Confirmation Dialog
                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            title = {
                                Text(
                                    text = "Exit Hayya?",
                                    style = TabularClockStyle.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            },
                            text = {
                                Text(
                                    text = "Scheduled prayer alarms and home screen widgets will continue running automatically in the background.",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showExitDialog = false
                                        finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Exit App", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showExitDialog = false }) {
                                    Text("Stay", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        )
                    }

                    // Item 4: Missing Permissions Dialog (prompts on open only if missing)
                    if (missingPermissions.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = { missingPermissionsState.value = emptyList() },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Permissions Required",
                                        style = TabularClockStyle.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Hayya requires the following permissions to guarantee exact Adhan alarms:",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    missingPermissions.forEach { permissionMsg ->
                                        Text(
                                            text = "• $permissionMsg",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        requestNextMissingPermission()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Grant Permissions", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { missingPermissionsState.value = emptyList() }) {
                                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Item 4: Check permissions on every open
        checkPermissionsState()

        val app = application as? HayyaApplication ?: HayyaApplication.instance
        lifecycleScope.launch {
            app.alarmScheduler.scheduleRollingAlarms()
            PrayerWidgetUpdater.updateAllWidgets(this@MainActivity)
        }
    }

    /**
     * Item 4: Evaluates permissions every time the app opens.
     * If all are granted, zero prompting occurs.
     */
    private fun checkPermissionsState() {
        val app = application as? HayyaApplication ?: HayyaApplication.instance
        val missing = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missing.add("Notification Permission (for Adhan alarms and prayer reminders)")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!app.alarmScheduler.canScheduleExactAlarms()) {
                missing.add("Exact Alarm Permission (for sub-second accuracy through Doze mode)")
            }
        }

        missingPermissionsState.value = missing
    }

    private fun requestNextMissingPermission() {
        val app = application as? HayyaApplication ?: HayyaApplication.instance

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !app.alarmScheduler.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            return
        }

        missingPermissionsState.value = emptyList()
    }
}
