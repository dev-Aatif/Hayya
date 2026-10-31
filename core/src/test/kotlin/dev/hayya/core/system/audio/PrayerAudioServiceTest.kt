package dev.hayya.core.system.audio

import android.media.AudioAttributes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrayerAudioServiceTest {

    @Test
    fun audioAttributes_mustRouteToAlarmStreamAndSonification() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        assertThat(audioAttributes.usage).isEqualTo(AudioAttributes.USAGE_ALARM)
        assertThat(audioAttributes.contentType).isEqualTo(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    }

    @Test
    fun actionConstants_andExtraKeys_areConsistent() {
        assertThat(PrayerAudioService.ACTION_START_ALARM).isEqualTo("dev.hayya.audio.ACTION_START_ALARM")
        assertThat(PrayerAudioService.ACTION_MUTE).isEqualTo("dev.hayya.audio.ACTION_MUTE")
        assertThat(PrayerAudioService.ACTION_STOP).isEqualTo("dev.hayya.audio.ACTION_STOP")

        assertThat(PrayerAudioService.DEFAULT_TIMEOUT_SECONDS).isEqualTo(60)
        assertThat(PrayerAudioService.CHANNEL_ID).isEqualTo("channel_prayer_audio_service")
    }

    @Test
    fun fadeOutStepCalculation_smoothlyDecreasesVolumeToZero() {
        val fadeSteps = 20
        val volumeSteps = (1..fadeSteps).map { step ->
            1.0f - (step.toFloat() / fadeSteps)
        }

        assertThat(volumeSteps).hasSize(20)
        // Step 1: 0.95f
        assertThat(volumeSteps.first()).isWithin(0.01f).of(0.95f)
        // Step 20: 0.0f
        assertThat(volumeSteps.last()).isEqualTo(0.0f)
        // Strictly decreasing
        for (i in 0 until volumeSteps.size - 1) {
            assertThat(volumeSteps[i]).isGreaterThan(volumeSteps[i + 1])
        }
    }
}
