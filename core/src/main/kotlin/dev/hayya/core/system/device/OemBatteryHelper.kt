package dev.hayya.core.system.device

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Utility helper managing aggressive OEM battery optimization exemptions and background autostart settings.
 *
 * Covers custom power management layers across Samsung (OneUI), Xiaomi (MIUI/HyperOS),
 * Huawei/Honor (EMUI/MagicOS), Oppo/Realme/OnePlus (ColorOS/OxygenOS), and Vivo (FuntouchOS/OriginOS).
 */
object OemBatteryHelper {

    private const val TAG = "OemBatteryHelper"

    /**
     * Enumeration of recognized OEM device manufacturers known for aggressive background process killing.
     */
    enum class OemVendor {
        SAMSUNG,
        XIAOMI,
        HUAWEI,
        OPPO,
        VIVO,
        TRANSSION,
        ASUS,
        STANDARD_AOSP
    }

    /**
     * Identifies the current device vendor from [Build.MANUFACTURER].
     */
    val currentVendor: OemVendor
        get() {
            val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
            return when {
                manufacturer.contains("samsung") -> OemVendor.SAMSUNG
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> OemVendor.XIAOMI
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> OemVendor.HUAWEI
                manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> OemVendor.OPPO
                manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> OemVendor.VIVO
                manufacturer.contains("transsion") || manufacturer.contains("tecno") || manufacturer.contains("infinix") || manufacturer.contains("itel") -> OemVendor.TRANSSION
                manufacturer.contains("asus") -> OemVendor.ASUS
                else -> OemVendor.STANDARD_AOSP
            }
        }

    /**
     * Returns true if the app is currently whitelisted from standard Android battery optimizations.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Creates an intent prompting the user to exempt this app from standard battery optimizations.
     */
    @SuppressLint("BatteryLife")
    fun createIgnoreBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
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
                    Log.i(TAG, "Launched APP_BATTERY_USAGE_SETTINGS directly")
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

        // 3. Fallback to general Ignore Battery Optimization list
        return try {
            val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(listIntent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to OEM autostart settings", e)
            launchAutostartSettings(context)
        }
    }

    /**
     * Attempts to launch the manufacturer-specific autostart or background restriction screen.
     * Falls back to general App Details Settings if OEM-specific screens cannot be resolved.
     *
     * @return true if an activity was successfully launched.
     */
    fun launchAutostartSettings(context: Context): Boolean {
        val candidateIntents = getAutostartIntents(context)

        for (intent in candidateIntents) {
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.i(TAG, "Launched OEM autostart activity: ${intent.component}")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to start intent: ${intent.component}", e)
                }
            }
        }

        // Fallback to standard application details settings
        return try {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
            Log.i(TAG, "Launched fallback application details settings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch fallback application details settings", e)
            false
        }
    }

    /**
     * Generates an ordered list of candidate OEM intent configurations for the active vendor.
     */
    fun getAutostartIntents(context: Context): List<Intent> {
        val intents = mutableListOf<Intent>()

        when (currentVendor) {
            OemVendor.XIAOMI -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.powerkeeper",
                            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                        )
                    ).putExtra("package_name", context.packageName)
                        .putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager))
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.powercenter.PowerSettings"
                        )
                    )
                )
            }
            OemVendor.SAMSUNG -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.ui.battery.BatteryActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.sm",
                            "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"
                        )
                    )
                )
                intents.add(Intent("com.samsung.android.sm.ACTION_BATTERY"))
            }
            OemVendor.HUAWEI -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.process.ProtectActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                        )
                    )
                )
            }
            OemVendor.OPPO -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.startupapp.StartupAppListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.oplus.battery",
                            "com.oplus.battery.OplusBatteryActivity"
                        )
                    )
                )
            }
            OemVendor.VIVO -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                        )
                    )
                )
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                        )
                    )
                )
            }
            OemVendor.TRANSSION -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.transsion.phonemanager",
                            "com.transsion.phonemanager.settings.AutoStartAppListActivity"
                        )
                    )
                )
            }
            OemVendor.ASUS -> {
                intents.add(
                    Intent().setComponent(
                        ComponentName(
                            "com.asus.mobilemanager",
                            "com.asus.mobilemanager.autostart.AutoStartActivity"
                        )
                    )
                )
            }
            OemVendor.STANDARD_AOSP -> {
                intents.add(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                )
            }
        }

        return intents
    }
}
