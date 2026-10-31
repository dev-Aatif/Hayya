package dev.hayya.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dev.hayya.core.HayyaApplication
import dev.hayya.core.system.device.OemBatteryHelper
import dev.hayya.core.ui.screens.MainDashboardScreen
import dev.hayya.core.ui.screens.OnboardingScreen
import dev.hayya.core.ui.screens.SettingsScreen
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.OledBackground
import dev.hayya.core.ui.viewmodel.PrayerViewModel

/**
 * Main application entry activity hosting the dashboard, settings, and onboarding flows.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as? HayyaApplication ?: HayyaApplication.instance

        // Request runtime notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule the initial rolling batch of exact alarms
        lifecycleScope.launch {
            app.alarmScheduler.scheduleRollingAlarms()
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
                    var showOnboarding by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        isBatteryIgnored = OemBatteryHelper.isIgnoringBatteryOptimizations(this@MainActivity)
                        isExactAlarmGranted = app.alarmScheduler.canScheduleExactAlarms()
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
                            onLaunchBatterySettings = {
                                OemBatteryHelper.launchAutostartSettings(this@MainActivity)
                                isBatteryIgnored = OemBatteryHelper.isIgnoringBatteryOptimizations(this@MainActivity)
                            }
                        )
                    } else if (showOnboarding) {
                        OnboardingScreen(
                            locationName = state.locationName,
                            selectedMadhab = state.userPreferences.madhab,
                            isBatteryOptimizationGranted = isBatteryIgnored,
                            isExactAlarmGranted = isExactAlarmGranted,
                            onRequestLocation = { requestLocationUpdates() },
                            onSelectMadhab = { viewModel.updateJuristicMethod(it) },
                            onRequestBatteryOptimization = {
                                val intent = OemBatteryHelper.createIgnoreBatteryOptimizationIntent(this@MainActivity)
                                startActivity(intent)
                                isBatteryIgnored = OemBatteryHelper.isIgnoringBatteryOptimizations(this@MainActivity)
                            },
                            onRequestExactAlarm = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:$packageName")
                                    })
                                }
                                isExactAlarmGranted = app.alarmScheduler.canScheduleExactAlarms()
                            },
                            onCompleteSetup = { showOnboarding = false }
                        )
                    } else {
                        MainDashboardScreen(
                            state = state,
                            onOpenSettings = { viewModel.setSettingsOpen(true) },
                            onToggleAudioMode = { viewModel.togglePrayerAudioMode(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check system permissions on return
        val app = application as? HayyaApplication ?: HayyaApplication.instance
        lifecycleScope.launch {
            app.alarmScheduler.scheduleRollingAlarms()
        }
    }

    private fun requestLocationUpdates() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun fetchDeviceLocation() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val lastLocation: Location? = try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else null
            } catch (se: SecurityException) {
                null
            }

            lastLocation?.let { loc ->
                viewModel.updateLocation(loc.latitude, loc.longitude, "Device Location")
            }
        } catch (_: Exception) {
            // Keep default coordinates if location is unavailable
        }
    }
}
