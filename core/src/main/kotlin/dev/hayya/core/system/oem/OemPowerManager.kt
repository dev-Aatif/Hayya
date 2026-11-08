package dev.hayya.core.system.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Enterprise-grade OEM background execution survival helper covering aggressive
 * power-management daemons on Samsung (OneUI), Xiaomi (MIUI/HyperOS), Huawei (EMUI/HarmonyOS),
 * Oppo/Realme/OnePlus (ColorOS/OxygenOS), and Vivo (FuntouchOS/OriginOS).
 */
object OemPowerManager {

    private const val TAG = "OemPowerManager"

    enum class Vendor {
        SAMSUNG,
        XIAOMI,
        HUAWEI,
        OPPO,
        VIVO,
        TRANSSION,
        ASUS,
        AOSP
    }

    val currentVendor: Vendor
        get() {
            val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
            return when {
                manufacturer.contains("samsung") -> Vendor.SAMSUNG
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> Vendor.XIAOMI
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> Vendor.HUAWEI
                manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> Vendor.OPPO
                manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> Vendor.VIVO
                manufacturer.contains("transsion") || manufacturer.contains("tecno") || manufacturer.contains("infinix") -> Vendor.TRANSSION
                manufacturer.contains("asus") -> Vendor.ASUS
                else -> Vendor.AOSP
            }
        }

    /**
     * Checks if the app is currently whitelisted from system battery optimizations.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Directly navigates to the app's Battery usage / App info screen where
     * "Unrestricted", "Optimized", and "Restricted" radio options reside.
     */
    fun openBatteryUsageSettings(context: Context): Boolean {
        // 1. Android 12+ (API 31+) dedicated App Battery Usage page
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val batteryUsageIntent = Intent("android.settings.APP_BATTERY_USAGE_SETTINGS").apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (batteryUsageIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(batteryUsageIntent)
                    Log.i(TAG, "Launched APP_BATTERY_USAGE_SETTINGS successfully")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "APP_BATTERY_USAGE_SETTINGS intent failed", e)
            }
        }

        // 2. Settings > Apps > App Info > Hayya (where Battery is 1 tap away)
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (appDetailsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appDetailsIntent)
                Log.i(TAG, "Launched ACTION_APPLICATION_DETAILS_SETTINGS")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "ACTION_APPLICATION_DETAILS_SETTINGS intent failed", e)
        }

        // 3. Fallback to OEM specific autostart management or system list
        return launchOemAutostartSettings(context)
    }

    /**
     * Launches OEM-specific background autostart or process protection screens.
     */
    fun launchOemAutostartSettings(context: Context): Boolean {
        val candidateIntents = getOemCandidateIntents(context)

        for (intent in candidateIntents) {
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.i(TAG, "Launched OEM activity: ${intent.component ?: intent.action}")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed launching OEM intent: ${intent.component}", e)
                }
            }
        }

        // Fallback to standard ignore battery optimization list
        return try {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
            true
        } catch (e: Exception) {
            Log.e(TAG, "All background settings intents failed", e)
            false
        }
    }

    private fun getOemCandidateIntents(context: Context): List<Intent> {
        val list = mutableListOf<Intent>()

        when (currentVendor) {
            Vendor.XIAOMI -> {
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    )
                )
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.powerkeeper",
                            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                        )
                    ).putExtra("package_name", context.packageName)
                )
            }
            Vendor.SAMSUNG -> {
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.battery.ui.BatteryActivity"
                        )
                    )
                )
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                    )
                )
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.sm",
                            "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"
                        )
                    )
                )
            }
            Vendor.HUAWEI -> {
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                    )
                )
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.process.ProtectActivity"
                        )
                    )
                )
            }
            Vendor.OPPO -> {
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    )
                )
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.startupapp.StartupAppListActivity"
                        )
                    )
                )
            }
            Vendor.VIVO -> {
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                        )
                    )
                )
                list.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                        )
                    )
                )
            }
            else -> {}
        }

        return list
    }
}
