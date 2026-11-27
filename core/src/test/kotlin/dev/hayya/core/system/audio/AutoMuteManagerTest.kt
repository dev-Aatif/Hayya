package dev.hayya.core.system.audio

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.domain.model.SavedRingerState
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AutoMuteManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val audioManager = mockk<AudioManager>(relaxed = true)
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val repository = mockk<UserPreferencesRepository>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns mockk(relaxed = true)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk(relaxed = true)

        mockkConstructor(NotificationCompat.Builder::class)
        every { anyConstructed<NotificationCompat.Builder>().build() } returns mockk(relaxed = true)

        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun activateAutoMute_recordsPreviousVolumeAndRingerMode_andSetsVibrateMode() = runTest {
        every { audioManager.ringerMode } returns AudioManager.RINGER_MODE_NORMAL
        every { audioManager.getStreamVolume(AudioManager.STREAM_RING) } returns 7
        every { audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) } returns 5

        val result = AutoMuteManager.activateAutoMute(
            context = context,
            durationMinutes = 20,
            targetMode = AutoMuteTargetMode.VIBRATE,
            repository = repository
        )

        assertThat(result).isTrue()
        coVerify(exactly = 1) {
            repository.saveRingerState(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                ringVolume = 7,
                notificationVolume = 5,
                endEpochMillis = match { it > System.currentTimeMillis() }
            )
        }
        verify(exactly = 1) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
        verify(exactly = 1) {
            notificationManager.notify(AutoMuteManager.NOTIFICATION_ID, any())
        }
    }

    @Test
    fun activateAutoMute_withSilentMode_whenDndGranted_setsSilentMode() = runTest {
        every { notificationManager.isNotificationPolicyAccessGranted } returns true
        every { audioManager.ringerMode } returns AudioManager.RINGER_MODE_NORMAL
        every { audioManager.getStreamVolume(AudioManager.STREAM_RING) } returns 10
        every { audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) } returns 8

        val result = AutoMuteManager.activateAutoMute(
            context = context,
            durationMinutes = 15,
            targetMode = AutoMuteTargetMode.SILENT,
            repository = repository
        )

        assertThat(result).isTrue()
        verify(exactly = 1) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    @Test
    fun activateAutoMute_withSilentMode_whenDndNotGranted_fallsBackToVibrateMode() = runTest {
        every { notificationManager.isNotificationPolicyAccessGranted } returns false
        every { audioManager.ringerMode } returns AudioManager.RINGER_MODE_NORMAL
        every { audioManager.getStreamVolume(AudioManager.STREAM_RING) } returns 6
        every { audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) } returns 4

        val result = AutoMuteManager.activateAutoMute(
            context = context,
            durationMinutes = 15,
            targetMode = AutoMuteTargetMode.SILENT,
            repository = repository
        )

        assertThat(result).isTrue()
        verify(exactly = 1) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
        verify(exactly = 0) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    @Test
    fun restoreRingerMode_restoresExactPreviousRingerModeAndStreamVolumes() = runTest {
        val saved = SavedRingerState(
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            ringVolume = 8,
            notificationVolume = 6,
            isAutoMuted = true,
            endEpochMillis = System.currentTimeMillis() + 600_000L
        )
        every { repository.savedRingerStateFlow } returns flowOf(saved)

        val result = AutoMuteManager.restoreRingerMode(context, repository)

        assertThat(result).isTrue()
        verify(exactly = 1) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
        verify(exactly = 1) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 8, 0)
        }
        verify(exactly = 1) {
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 6, 0)
        }
        verify(exactly = 1) {
            notificationManager.cancel(AutoMuteManager.NOTIFICATION_ID)
        }
        coVerify(exactly = 1) {
            repository.clearAutoMuteState()
        }
    }

    @Test
    fun restoreRingerMode_whenNotAutoMuted_doesNothingAndReturnsFalse() = runTest {
        val saved = SavedRingerState(
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            ringVolume = 5,
            notificationVolume = 5,
            isAutoMuted = false,
            endEpochMillis = 0L
        )
        every { repository.savedRingerStateFlow } returns flowOf(saved)

        val result = AutoMuteManager.restoreRingerMode(context, repository)

        assertThat(result).isFalse()
        verify(exactly = 0) {
            audioManager.ringerMode = any()
            audioManager.setStreamVolume(any(), any(), any())
            notificationManager.cancel(any())
        }
        coVerify(exactly = 0) {
            repository.clearAutoMuteState()
        }
    }

    @Test
    fun restoreRingerMode_whenSecurityExceptionThrown_preservesSavedStateAndReturnsFalse() = runTest {
        val saved = SavedRingerState(
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            ringVolume = 8,
            notificationVolume = 6,
            isAutoMuted = true,
            endEpochMillis = System.currentTimeMillis() - 60_000L
        )
        every { repository.savedRingerStateFlow } returns flowOf(saved)
        every { audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL } throws SecurityException("DND access revoked")

        val result = AutoMuteManager.restoreRingerMode(context, repository)

        assertThat(result).isFalse()
        verify(exactly = 0) {
            notificationManager.cancel(AutoMuteManager.NOTIFICATION_ID)
        }
        verify(exactly = 1) {
            notificationManager.notify(AutoMuteManager.RESTORE_FAILED_NOTIFICATION_ID, any())
        }
        coVerify(exactly = 0) {
            repository.clearAutoMuteState()
        }
    }

    @Test
    fun activateAutoMute_withFridayJumuah_activatesCorrectlyAndPostsFridayNotification() = runTest {
        every { audioManager.ringerMode } returns AudioManager.RINGER_MODE_NORMAL
        every { audioManager.getStreamVolume(AudioManager.STREAM_RING) } returns 6
        every { audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) } returns 4

        val result = AutoMuteManager.activateAutoMute(
            context = context,
            durationMinutes = 45,
            targetMode = AutoMuteTargetMode.VIBRATE,
            repository = repository,
            isFridayJumuah = true
        )

        assertThat(result).isTrue()
        coVerify(exactly = 1) {
            repository.saveRingerState(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                ringVolume = 6,
                notificationVolume = 4,
                endEpochMillis = match { it >= System.currentTimeMillis() + 44 * 60_000L }
            )
        }
        verify(exactly = 1) {
            notificationManager.notify(AutoMuteManager.NOTIFICATION_ID, any())
        }
    }

    @Test
    fun activateAutoMute_withZeroOrNegativeDuration_returnsFalseWithoutAction() = runTest {
        val resultZero = AutoMuteManager.activateAutoMute(
            context = context,
            durationMinutes = 0,
            targetMode = AutoMuteTargetMode.SILENT,
            repository = repository
        )
        assertThat(resultZero).isFalse()

        val resultNegative = AutoMuteManager.activateAutoMute(
            context = context,
            durationMinutes = -15,
            targetMode = AutoMuteTargetMode.VIBRATE,
            repository = repository
        )
        assertThat(resultNegative).isFalse()
    }

    @Test
    fun activateAutoMute_whenAlreadyAutoMuted_preservesOriginalRingerModeAndVolumes() = runTest {
        val existingOriginal = SavedRingerState(
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            ringVolume = 9,
            notificationVolume = 7,
            isAutoMuted = true,
            endEpochMillis = System.currentTimeMillis() + 300_000L
        )
        every { repository.savedRingerStateFlow } returns flowOf(existingOriginal)
        // Device is currently silenced by the active auto-mute
        every { audioManager.ringerMode } returns AudioManager.RINGER_MODE_VIBRATE
        every { audioManager.getStreamVolume(AudioManager.STREAM_RING) } returns 0
        every { audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) } returns 0

        val result = AutoMuteManager.activateAutoMute(
            context = context,
            durationMinutes = 45,
            targetMode = AutoMuteTargetMode.VIBRATE,
            repository = repository,
            isFridayJumuah = true
        )

        assertThat(result).isTrue()
        coVerify(exactly = 1) {
            repository.saveRingerState(
                ringerMode = AudioManager.RINGER_MODE_NORMAL,
                ringVolume = 9,
                notificationVolume = 7,
                endEpochMillis = match { it >= System.currentTimeMillis() + 44 * 60_000L }
            )
        }
    }
}
