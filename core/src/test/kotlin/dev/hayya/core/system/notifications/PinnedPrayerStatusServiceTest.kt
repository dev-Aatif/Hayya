package dev.hayya.core.system.notifications

import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class PinnedPrayerStatusServiceTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun companionConstants_matchSpecifications() {
        assertThat(PinnedPrayerStatusService.NOTIFICATION_ID).isEqualTo(88001)
        assertThat(PinnedPrayerStatusService.CHANNEL_ID).isEqualTo("channel_pinned_prayer_status")
        assertThat(PinnedPrayerStatusService.ACTION_START).isEqualTo("dev.hayya.notification.ACTION_START_PINNED_STATUS")
        assertThat(PinnedPrayerStatusService.ACTION_STOP).isEqualTo("dev.hayya.notification.ACTION_STOP_PINNED_STATUS")
    }

    @Test
    fun start_dispatchesStartIntent() {
        val intentSlot = slot<Intent>()

        PinnedPrayerStatusService.start(context)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            verify(atLeast = 1) {
                context.startForegroundService(capture(intentSlot))
            }
        } else {
            verify(atLeast = 1) {
                context.startService(capture(intentSlot))
            }
        }
        assertThat(intentSlot.isCaptured).isTrue()
    }

    @Test
    fun stop_dispatchesStopIntent() {
        val intentSlot = slot<Intent>()

        PinnedPrayerStatusService.stop(context)

        verify(atLeast = 1) {
            context.startService(capture(intentSlot))
        }
        assertThat(intentSlot.isCaptured).isTrue()
    }
}
