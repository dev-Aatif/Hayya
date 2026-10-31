package dev.hayya.core.system.device

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class OemBatteryHelperTest {

    private val context = mockk<Context>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)

    @Test
    fun isIgnoringBatteryOptimizations_delegatesToPowerManager() {
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "dev.hayya.test"
        every { powerManager.isIgnoringBatteryOptimizations("dev.hayya.test") } returns true

        val isIgnoring = OemBatteryHelper.isIgnoringBatteryOptimizations(context)
        assertThat(isIgnoring).isTrue()
    }

    @Test
    fun createIgnoreBatteryOptimizationIntent_hasCorrectActionAndPackageData() {
        every { context.packageName } returns "dev.hayya.test"

        val intent = OemBatteryHelper.createIgnoreBatteryOptimizationIntent(context)

        assertThat(intent.action).isEqualTo(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        assertThat(intent.data?.scheme).isEqualTo("package")
        assertThat(intent.data?.schemeSpecificPart).isEqualTo("dev.hayya.test")
    }

    @Test
    fun getAutostartIntents_returnsNonEmptyCandidateListForRecognizedVendors() {
        val intents = OemBatteryHelper.getAutostartIntents(context)

        // Must always return at least one fallback or OEM intent
        assertThat(intents).isNotEmpty()
    }
}
