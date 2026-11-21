package dev.hayya.core.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.hayya.core.HayyaApplication
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.system.notifications.PinnedPrayerStatusService
import dev.hayya.core.system.oem.OemPowerManager
import dev.hayya.core.ui.components.AppNavTab
import dev.hayya.core.ui.components.FloatingNavBar
import dev.hayya.core.ui.screens.FridayGuideScreen
import dev.hayya.core.ui.screens.MainDashboardScreen
import dev.hayya.core.ui.screens.OnboardingScreen
import dev.hayya.core.ui.screens.PrayerTrackerScreen
import dev.hayya.core.ui.screens.QiblaScreen
import dev.hayya.core.ui.screens.RamadanScreen
import dev.hayya.core.ui.screens.SettingsScreen
import dev.hayya.core.ui.screens.TahajjudScreen
import dev.hayya.core.ui.screens.TasbihScreen
import dev.hayya.core.ui.screens.ToolsScreen
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.viewmodel.PrayerViewModel
import dev.hayya.core.widget.PrayerWidgetUpdater
import kotlinx.coroutines.launch

private enum class ActiveDestination {
    ONBOARDING,
    RAMADAN,
    TASBIH,
    QIBLA,
    TRACKER,
    TAHAJJUD,
    FRIDAY_GUIDE,
    MAIN_TABS
}

/**
 * Main application entry activity hosting the dashboard, dedicated spiritual pages,
 * settings, and guided onboarding flows.
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

    private val missingPermissionsState = mutableStateOf<List<String>>(emptyList())
    private val isBatteryIgnoredState = mutableStateOf(false)
    private val isExactAlarmGrantedState = mutableStateOf(true)
    private val isNotificationGrantedState = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as? HayyaApplication ?: HayyaApplication.instance

        checkPermissionsState()
        handleWidgetNavigationIntent(intent)

        lifecycleScope.launch {
            app.alarmScheduler.scheduleRollingAlarms()
            PrayerWidgetUpdater.updateAllWidgets(this@MainActivity)
        }

        // Initialize ViewModel immediately so DataStore begins emitting before first draw
        val vm = viewModel
        val contentView = findViewById<View>(android.R.id.content)
        contentView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (vm.uiState.value.isPreferencesLoaded) {
                        contentView.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode = state.userPreferences.themeMode
            val dynamicColor = state.userPreferences.dynamicColorEnabled
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeModeOption.DARK -> true
                ThemeModeOption.LIGHT -> false
                ThemeModeOption.SYSTEM -> systemInDark
            }

            LaunchedEffect(isDarkTheme) {
                val barStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            HayyaTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isBatteryIgnored by isBatteryIgnoredState
                    val isExactAlarmGranted by isExactAlarmGrantedState
                    val isNotificationGranted by isNotificationGrantedState
                    var showExitDialog by remember { mutableStateOf(false) }
                    val missingPermissions by missingPermissionsState

                    LaunchedEffect(state.isPreferencesLoaded, state.userPreferences.pinnedNotificationEnabled) {
                        if (!state.isPreferencesLoaded) return@LaunchedEffect
                        if (state.userPreferences.pinnedNotificationEnabled) {
                            PinnedPrayerStatusService.start(this@MainActivity)
                        } else {
                            PinnedPrayerStatusService.stop(this@MainActivity)
                        }
                    }

                    // Keep all home screen widgets in live sync whenever widget-relevant preferences or prayer logs change
                    LaunchedEffect(
                        state.isPreferencesLoaded,
                        state.userPreferences.location,
                        state.userPreferences.calculationMethod,
                        state.userPreferences.madhab,
                        state.userPreferences.highLatitudeRule,
                        state.userPreferences.ishaExpiresAtMidnight,
                        state.userPreferences.manualMinuteOffsets,
                        state.userPreferences.themeMode,
                        state.todayPrayerLog
                    ) {
                        if (!state.isPreferencesLoaded) return@LaunchedEffect
                        PrayerWidgetUpdater.updateAllWidgets(this@MainActivity)
                    }

                    if (!state.isPreferencesLoaded) {
                        Box(modifier = Modifier.fillMaxSize())
                        return@Surface
                    }

                    val showOnboarding = !state.userPreferences.hasCompletedOnboarding || state.isOnboardingForced

                    val destination = when {
                        showOnboarding -> ActiveDestination.ONBOARDING
                        state.isRamadanOpen -> ActiveDestination.RAMADAN
                        state.isTasbihOpen -> ActiveDestination.TASBIH
                        state.isQiblaOpen -> ActiveDestination.QIBLA
                        state.isTrackerOpen -> ActiveDestination.TRACKER
                        state.isTahajjudOpen -> ActiveDestination.TAHAJJUD
                        state.isFridayGuideOpen -> ActiveDestination.FRIDAY_GUIDE
                        else -> ActiveDestination.MAIN_TABS
                    }

                    BackHandler(enabled = state.isOnboardingForced) {
                        viewModel.completeOnboarding()
                    }
                    BackHandler(enabled = state.isRamadanOpen) { viewModel.setRamadanOpen(false) }
                    BackHandler(enabled = state.isTasbihOpen) { viewModel.setTasbihOpen(false) }
                    BackHandler(enabled = state.isQiblaOpen) { viewModel.setQiblaOpen(false) }
                    BackHandler(enabled = state.isTrackerOpen) { viewModel.setTrackerOpen(false) }
                    BackHandler(enabled = state.isTahajjudOpen) { viewModel.setTahajjudOpen(false) }
                    BackHandler(enabled = state.isFridayGuideOpen) { viewModel.setFridayGuideOpen(false) }

                    BackHandler(
                        enabled = destination == ActiveDestination.MAIN_TABS && state.currentTab != AppNavTab.DASHBOARD
                    ) {
                        viewModel.selectNavTab(AppNavTab.DASHBOARD)
                    }

                    BackHandler(
                        enabled = destination == ActiveDestination.MAIN_TABS && state.currentTab == AppNavTab.DASHBOARD
                    ) {
                        showExitDialog = true
                    }

                    Crossfade(
                        targetState = destination,
                        animationSpec = tween(durationMillis = 220),
                        label = "root_navigation"
                    ) { target ->
                        when (target) {
                            ActiveDestination.ONBOARDING -> {
                                OnboardingScreen(
                                    locationName = state.locationName,
                                    selectedMadhab = state.userPreferences.madhab,
                                    isBatteryOptimizationGranted = isBatteryIgnored,
                                    isExactAlarmGranted = isExactAlarmGranted,
                                    isNotificationGranted = isNotificationGranted,
                                    onSelectCity = { viewModel.selectCity(it) },
                                    onSelectMadhab = { viewModel.updateJuristicMethod(it) },
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onRequestBatteryOptimization = {
                                        OemPowerManager.openBatteryUsageSettings(this@MainActivity)
                                    },
                                    onRequestExactAlarm = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:$packageName")
                                            }
                                            startActivity(intent)
                                        }
                                    },
                                    onCompleteSetup = { viewModel.completeOnboarding() }
                                )
                            }

                            ActiveDestination.RAMADAN -> {
                                RamadanScreen(
                                    state = state,
                                    calculationEngine = app.calculationEngine,
                                    onBackClick = { viewModel.setRamadanOpen(false) },
                                    onToggleTodayFast = { viewModel.toggleTodayFast() },
                                    onSetFastStatus = { date, status -> viewModel.setFastStatus(date, status) },
                                    onCycleFastStatus = { date -> viewModel.cycleFastStatus(date) },
                                    onUpdateQadhaMadeUp = { viewModel.updateQadhaMadeUp(it) },
                                    onUpdateSuhoorAlarm = { enabled, mins -> viewModel.updateSuhoorAlarm(enabled, mins) },
                                    onUpdateIftarAlert = { viewModel.updateIftarAlert(it) }
                                )
                            }

                            ActiveDestination.TASBIH -> {
                                TasbihScreen(
                                    count = state.tasbihCount,
                                    targetMode = state.tasbihMode,
                                    onIncrement = { viewModel.incrementTasbih() },
                                    onReset = { viewModel.resetTasbih() },
                                    onSelectMode = { viewModel.setTasbihMode(it) },
                                    onBackClick = { viewModel.setTasbihOpen(false) }
                                )
                            }

                            ActiveDestination.QIBLA -> {
                                QiblaScreen(
                                    locationName = state.locationName,
                                    latitude = state.userPreferences.location.latitude,
                                    longitude = state.userPreferences.location.longitude,
                                    onBackClick = { viewModel.setQiblaOpen(false) }
                                )
                            }

                            ActiveDestination.TRACKER -> {
                                PrayerTrackerScreen(
                                    state = state,
                                    onBackClick = { viewModel.setTrackerOpen(false) },
                                    onTogglePrayerCompletion = { viewModel.togglePrayerCompletion(it) }
                                )
                            }

                            ActiveDestination.TAHAJJUD -> {
                                TahajjudScreen(
                                    state = state,
                                    onBackClick = { viewModel.setTahajjudOpen(false) },
                                    onToggleTahajjudAlarm = { viewModel.toggleTahajjudAlarm(it) }
                                )
                            }

                            ActiveDestination.FRIDAY_GUIDE -> {
                                val fridayMinutes = state.userPreferences.fridayAutoMuteDuration
                                    .resolveMinutes(state.userPreferences.autoMuteDuration)
                                    .let { if (it > 0) it else 45 }
                                FridayGuideScreen(
                                    isSurahKahfRead = state.isSurahKahfRead,
                                    isAutoMuted = state.savedRingerState.isAutoMuted,
                                    autoMuteEndEpochMillis = state.savedRingerState.endEpochMillis,
                                    fridayMinutes = fridayMinutes,
                                    salawatCount = state.salawatCount,
                                    onBackClick = { viewModel.setFridayGuideOpen(false) },
                                    onToggleSurahKahfRead = { viewModel.toggleSurahKahfRead() },
                                    onActivateKhutbahMute = { viewModel.activateKhutbahAutoMute(this@MainActivity) },
                                    onRestoreRingerMode = { viewModel.restoreNormalRingerMode(this@MainActivity) },
                                    onIncrementSalawat = { viewModel.incrementSalawat() }
                                )
                            }

                            ActiveDestination.MAIN_TABS -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Crossfade(
                                        targetState = state.currentTab,
                                        animationSpec = tween(durationMillis = 180),
                                        label = "tab_crossfade"
                                    ) { tab ->
                                        when (tab) {
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
                                                    onRestoreRingerMode = { viewModel.restoreNormalRingerMode(this@MainActivity) },
                                                    onOpenTracker = { viewModel.setTrackerOpen(true) },
                                                    onOpenTahajjud = { viewModel.setTahajjudOpen(true) },
                                                    onOpenFridayGuide = { viewModel.setFridayGuideOpen(true) },
                                                    onSelectCity = { viewModel.selectCity(it) }
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
                                                    },
                                                    onTriggerTestAlarm = { delaySeconds ->
                                                        viewModel.scheduleTestAlarm(delaySeconds)
                                                    },
                                                    onOpenTracker = { viewModel.setTrackerOpen(true) },
                                                    onOpenTahajjud = { viewModel.setTahajjudOpen(true) },
                                                    onOpenFridayGuide = { viewModel.setFridayGuideOpen(true) }
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
                                                    },
                                                    onTriggerTestAlarm = { delaySeconds ->
                                                        viewModel.scheduleTestAlarm(delaySeconds)
                                                    },
                                                    onSelectCity = { viewModel.selectCity(it) },
                                                    onOpenSetupGuide = { viewModel.openOnboarding() }
                                                )
                                            }
                                        }
                                    }

                                    FloatingNavBar(
                                        currentTab = state.currentTab,
                                        onTabSelected = { viewModel.selectNavTab(it) },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                }
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
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text(
                                    text = "Leave Hayya?",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            },
                            text = {
                                Text(
                                    text = "Your prayer alarms and widgets will keep running automatically.",
                                    style = MaterialTheme.typography.bodyMedium,
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
                                    Text("Exit", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showExitDialog = false }) {
                                    Text("Stay", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        )
                    }

                    // Missing Permissions Dialog (only shown when onboarding is already completed)
                    if (!showOnboarding && missingPermissions.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = { missingPermissionsState.value = emptyList() },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(20.dp),
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
                                        text = "Permissions Needed",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Allow these permissions so prayer alarms ring on time:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    missingPermissions.forEach { permissionMsg ->
                                        Text(
                                            text = "• $permissionMsg",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { requestNextMissingPermission() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Allow", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { missingPermissionsState.value = emptyList() }) {
                                    Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetNavigationIntent(intent)
    }

    private fun handleWidgetNavigationIntent(intent: Intent?) {
        when (intent?.getStringExtra(PrayerWidgetUpdater.EXTRA_START_DESTINATION)) {
            PrayerWidgetUpdater.DEST_TRACKER -> {
                viewModel.selectNavTab(AppNavTab.DASHBOARD)
                viewModel.setTrackerOpen(true)
            }
            PrayerWidgetUpdater.DEST_TAHAJJUD -> {
                viewModel.selectNavTab(AppNavTab.DASHBOARD)
                viewModel.setTahajjudOpen(true)
            }
            PrayerWidgetUpdater.DEST_RAMADAN -> {
                viewModel.selectNavTab(AppNavTab.DASHBOARD)
                viewModel.setRamadanOpen(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
        viewModel.refreshNow()

        val app = application as? HayyaApplication ?: HayyaApplication.instance
        lifecycleScope.launch {
            app.alarmScheduler.scheduleRollingAlarms()
            PrayerWidgetUpdater.updateAllWidgets(this@MainActivity)
        }
    }

    private fun checkPermissionsState() {
        val app = application as? HayyaApplication ?: HayyaApplication.instance
        isBatteryIgnoredState.value = OemPowerManager.isBatteryOptimizationIgnored(this)
        isExactAlarmGrantedState.value = app.alarmScheduler.canScheduleExactAlarms()
        val missing = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            isNotificationGrantedState.value = notifGranted
            if (!notifGranted) {
                missing.add("Notifications (for prayer alerts)")
            }
        } else {
            isNotificationGrantedState.value = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isExactAlarmGrantedState.value) {
                missing.add("Exact Alarms (to ring at the exact minute)")
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
