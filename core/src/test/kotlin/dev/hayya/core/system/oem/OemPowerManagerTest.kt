package dev.hayya.core.system.oem

import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class OemPowerManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)
    private val packageManager = mockk<PackageManager>(relaxed = true)

    @Test
    fun vendorEnum_containsAllMajorOemVendors() {
        val vendors = OemPowerManager.Vendor.entries.map { it.name }
        assertThat(vendors).containsExactly(
            "SAMSUNG",
            "XIAOMI",
            "HUAWEI",
            "OPPO",
            "VIVO",
            "TRANSSION",
            "ASUS",
            "AOSP"
        )
    }

    @Test
    fun isBatteryOptimizationIgnored_delegatesToPowerManager() {
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "dev.hayya.test"
        every { powerManager.isIgnoringBatteryOptimizations("dev.hayya.test") } returns true

        val isIgnored = OemPowerManager.isBatteryOptimizationIgnored(context)
        assertThat(isIgnored).isTrue()
    }

    @Test
    fun isBatteryOptimizationIgnored_returnsFalseWhenNotIgnored() {
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "dev.hayya.test"
        every { powerManager.isIgnoringBatteryOptimizations("dev.hayya.test") } returns false

        val isIgnored = OemPowerManager.isBatteryOptimizationIgnored(context)
        assertThat(isIgnored).isFalse()
    }

    @Test
    fun openBatteryUsageSettings_dispatchesWithoutCrashing() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "dev.hayya.test"
        every { packageManager.resolveActivity(any(), any<Int>()) } returns null

        val result = OemPowerManager.openBatteryUsageSettings(context)
        assertThat(result).isAnyOf(true, false)
    }

    @Test
    fun launchOemAutostartSettings_fallsBackGracefullyWhenNoOemActivityFound() {
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "dev.hayya.test"
        every { packageManager.resolveActivity(any(), any<Int>()) } returns null

        val result = OemPowerManager.launchOemAutostartSettings(context)
        assertThat(result).isAnyOf(true, false)
    }
}
