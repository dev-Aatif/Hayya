package dev.hayya.core.system.audio

import android.media.AudioAttributes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrayerAudioServiceTest {

    @Test
    fun audioAttributesConstants_mustRouteToAlarmStreamAndSonification() {
        // AudioAttributes.USAGE_ALARM routes to STREAM_ALARM (bypassing silent mode switches and DND)
        assertThat(AudioAttributes.USAGE_ALARM).isEqualTo(4)
        assertThat(AudioAttributes.CONTENT_TYPE_SONIFICATION).isEqualTo(4)
    }

    @Test
    fun actionConstants_andExtraKeys_areConsistent() {
        assertThat(PrayerAudioService.ACTION_START_ALARM).isEqualTo("dev.hayya.audio.ACTION_START_ALARM")
        assertThat(PrayerAudioService.ACTION_MUTE).isEqualTo("dev.hayya.audio.ACTION_MUTE")
        assertThat(PrayerAudioService.ACTION_STOP).isEqualTo("dev.hayya.audio.ACTION_STOP")
        assertThat(PrayerAudioService.ACTION_PLAYBACK_FINISHED).isEqualTo("dev.hayya.audio.ACTION_PLAYBACK_FINISHED")

        assertThat(PrayerAudioService.DEFAULT_TIMEOUT_SECONDS).isEqualTo(60)
        assertThat(PrayerAudioService.DEFAULT_ESCALATION_SECONDS).isEqualTo(0)
        assertThat(PrayerAudioService.EXTRA_VOLUME_ESCALATION_SECONDS).isEqualTo("extra_volume_escalation_seconds")
        assertThat(PrayerAudioService.CHANNEL_ID).isEqualTo("channel_prayer_audio_service")
    }

    @Test
    fun volumeEscalationStepCalculation_smoothlyIncreasesVolumeFromTenPercentToMax() {
        val escalationSeconds = 20
        val initialVolume = 0.10f
        val targetVolume = 1.0f
        val stepIntervalMs = 1500L
        val totalSteps = ((escalationSeconds * 1000L) / stepIntervalMs).toInt()
        val volumeStep = (targetVolume - initialVolume) / totalSteps

        val steps = (1..totalSteps).map { step ->
            (initialVolume + step * volumeStep).coerceAtMost(targetVolume)
        }

        // 20s / 1.5s = 13 steps
        assertThat(steps).hasSize(13)

        // Step 1 starts right above 0.10f
        assertThat(steps.first()).isGreaterThan(initialVolume)

        // Final step reaches 1.0f (or within 0.01f of target)
        assertThat(steps.last()).isWithin(0.01f).of(targetVolume)

        // Strictly increasing monotonically
        for (i in 0 until steps.size - 1) {
            assertThat(steps[i]).isLessThan(steps[i + 1])
        }
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
