package dev.hayya.core.system.receivers

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class SystemClockChangedReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)
    private val wakeLock = mockk<PowerManager.WakeLock>(relaxed = true)

    private lateinit var receiver: SystemClockChangedReceiver

    @Before
    fun setUp() {
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { powerManager.newWakeLock(any(), any()) } returns wakeLock
        receiver = SystemClockChangedReceiver()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun bootAndClockReceiver_isSubclassOfSystemClockChangedReceiver() {
        val bootAndClockReceiver = BootAndClockReceiver()
        assertThat(bootAndClockReceiver).isInstanceOf(SystemClockChangedReceiver::class.java)
    }

    @Test
    fun onReceive_acquiresWakeLockForSystemClockEvents() {
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns Intent.ACTION_TIME_CHANGED
        }

        receiver.onReceive(context, intent)

        verify(atLeast = 1) {
            wakeLock.acquire(15_000L) // 15-second safety timeout
        }
    }
}
