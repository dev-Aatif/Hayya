package dev.hayya.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
import dev.hayya.core.ui.screens.MainDashboardScreen
import dev.hayya.core.ui.screens.OnboardingScreen
import dev.hayya.core.ui.screens.SettingsScreen
import dev.hayya.core.ui.theme.CardBorderColor
import dev.hayya.core.ui.theme.ElevatedCardSurface
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.OledBackground
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TextPrimary
import dev.hayya.core.ui.theme.TextSecondary
import dev.hayya.core.ui.theme.WarmSandAccent
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

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchDeviceLocation()
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
                    alarmScheduler = app.alarmScheduler
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
            HayyaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OledBackground
                ) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    var isBatteryIgnored by remember {
                        mutableStateOf(OemBatteryHelper.isIgnoringBatteryOptimizations(this@MainActivity))
                    }
                    var isExactAlarmGranted by remember {
                        mutableStateOf(app.alarmScheduler.canScheduleExactAlarms())
                    }
                    var showExitDialog by remember { mutableStateOf(false) }
                    val missingPermissions by missingPermissionsState

                    LaunchedEffect(Unit) {
                        isBatteryIgnored = OemBatteryHelper.isIgnoringBatteryOptimizations(this@MainActivity)
                        isExactAlarmGranted = app.alarmScheduler.canScheduleExactAlarms()
                        checkPermissionsState()
                        fetchDeviceLocation()
                    }

                    // Item 1: Back Button in Settings navigates back to Dashboard
                    BackHandler(enabled = state.isSettingsOpen) {
                        viewModel.setSettingsOpen(false)
                    }

                    // Item 2: Back Button on Home asks first before closing
                    BackHandler(enabled = !state.isSettingsOpen) {
                        showExitDialog = true
                    }

                    if (state.isSettingsOpen) {
                        SettingsScreen(
                            userPreferences = state.userPreferences,
                            isBatteryOptimizationIgnored = isBatteryIgnored,
                            onBackClick = { viewModel.setSettingsOpen(false) },
                            onSelectAutoSilence = { viewModel.updateAutoSilence(it) },
                            onSelectMadhab = { viewModel.updateJuristicMethod(it) },
                            onSelectCalculationMethod = { viewModel.updateCalculationMethod(it) },
                            onToggleIshaMidnight = { viewModel.updateIshaExpiresAtMidnight(it) },
                            onTogglePreReminder = { viewModel.togglePreReminder(it) },
                            onToggleLastCall = { viewModel.toggleLastCall(it) },
                            onSelectHighLatitudeRule = { viewModel.updateHighLatitudeRule(it) },
                            onUpdatePrayerOffset = { prayer, offset ->
                                viewModel.updatePrayerOffset(prayer, offset)
                            },
                            onLaunchBatterySettings = {
                                // Item 3: Directly opens App battery usage / App Info
                                OemBatteryHelper.openBatteryUsageSettings(this@MainActivity)
                                isBatteryIgnored = OemBatteryHelper.isIgnoringBatteryOptimizations(this@MainActivity)
                            },
                            onTriggerTestAlarm = { delaySeconds ->
                                // Item 8: Test Alarm Simulator
                                viewModel.scheduleTestAlarm(delaySeconds)
                            }
                        )
                    } else {
                        MainDashboardScreen(
                            state = state,
                            onOpenSettings = { viewModel.setSettingsOpen(true) },
                            onToggleAudioMode = { viewModel.togglePrayerAudioMode(it) }
                        )
                    }

                    // Item 2: Exit Confirmation Dialog
                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            containerColor = ElevatedCardSurface,
                            titleContentColor = TextPrimary,
                            textContentColor = TextSecondary,
                            shape = RoundedCornerShape(16.dp),
                            title = {
                                Text(
                                    text = "Exit Hayya?",
                                    style = TabularClockStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                )
                            },
                            text = {
                                Text(
                                    text = "Scheduled prayer alarms and home screen widgets will continue running automatically in the background.",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = TextSecondary
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showExitDialog = false
                                        finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmSandAccent)
                                ) {
                                    Text("Exit App", color = Color(0xFF141619), fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showExitDialog = false }) {
                                    Text("Stay", color = TextPrimary)
                                }
                            }
                        )
                    }

                    // Item 4: Missing Permissions Dialog (prompts on open only if missing)
                    if (missingPermissions.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = { missingPermissionsState.value = emptyList() },
                            containerColor = ElevatedCardSurface,
                            shape = RoundedCornerShape(16.dp),
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = WarmSandAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Permissions Required",
                                        style = TabularClockStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Hayya requires the following permissions to guarantee exact Adhan alarms:",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    missingPermissions.forEach { permissionMsg ->
                                        Text(
                                            text = "• $permissionMsg",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        requestNextMissingPermission()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmSandAccent)
                                ) {
                                    Text("Grant Permissions", color = Color(0xFF141619), fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { missingPermissionsState.value = emptyList() }) {
                                    Text("Dismiss", color = TextSecondary)
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

    private fun requestLocationUpdates() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Item 6: Resolves real city and country using Geocoder or sets canonical region.
     */
    private fun fetchDeviceLocation() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val hasFine = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (!hasFine && !hasCoarse) return@launch

                val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@launch
                val lastLocation: Location? = try {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (se: SecurityException) {
                    null
                }

                lastLocation?.let { loc ->
                    var resolvedName: String? = null
                    try {
                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                            val country = addr.countryName
                            resolvedName = if (city != null && country != null) {
                                "$city, $country"
                            } else {
                                city ?: country
                            }
                        }
                    } catch (_: Exception) {
                        // Geocoder service unavailable (e.g. offline)
                    }

                    val finalLocationName = resolvedName ?: "Current Location"
                    withContext(Dispatchers.Main) {
                        viewModel.updateLocation(loc.latitude, loc.longitude, finalLocationName)
                    }
                }
            } catch (_: Exception) {
                // Keep default convention location if resolution fails
            }
        }
    }
}
