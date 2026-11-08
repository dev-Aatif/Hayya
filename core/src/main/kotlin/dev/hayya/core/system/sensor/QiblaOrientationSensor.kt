package dev.hayya.core.system.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dev.hayya.core.domain.qibla.QiblaCalculator

/**
 * Offline orientation sensor provider using Android's [SensorManager].
 *
 * Primary Strategy:
 * - Listens to [Sensor.TYPE_ROTATION_VECTOR] for hardware/software sensor fusion
 *   providing accurate 3D orientation without gimbal lock.
 *
 * Fallback Strategy:
 * - If rotation vector sensor is not present, falls back to combining
 *   [Sensor.TYPE_ACCELEROMETER] and [Sensor.TYPE_MAGNETIC_FIELD].
 *
 * Incorporates continuous circular angle low-pass filtering to guarantee
 * smooth compass movement without jitter or 0°/360° jump artifacts.
 */
class QiblaOrientationSensor(
    context: Context,
    private val smoothingAlpha: Float = 0.2f
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometerReading = false
    private var hasMagnetometerReading = false

    private var filteredAzimuth = 0f
    private var isFirstReading = true

    private var onOrientationChanged: ((azimuth: Float, accuracy: Int) -> Unit)? = null

    /**
     * Checks whether the host device possesses the required hardware sensors to calculate orientation.
     */
    fun hasRequiredSensors(): Boolean {
        return rotationVectorSensor != null || (accelerometerSensor != null && magneticSensor != null)
    }

    /**
     * Begins listening to orientation sensor events.
     */
    fun start(callback: (azimuth: Float, accuracy: Int) -> Unit) {
        onOrientationChanged = callback
        isFirstReading = true

        val sm = sensorManager ?: return

        if (rotationVectorSensor != null) {
            sm.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometerSensor?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magneticSensor?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    /**
     * Unregisters sensor listeners and cleans up resources.
     */
    fun stop() {
        sensorManager?.unregisterListener(this)
        onOrientationChanged = null
        hasAccelerometerReading = false
        hasMagnetometerReading = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        var rawAzimuth: Float? = null

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                rawAzimuth = ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).toFloat()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                hasAccelerometerReading = true
                if (hasMagnetometerReading) {
                    rawAzimuth = computeAzimuthFromGravityAndMagnetic()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                hasMagnetometerReading = true
                if (hasAccelerometerReading) {
                    rawAzimuth = computeAzimuthFromGravityAndMagnetic()
                }
            }
        }

        rawAzimuth?.let { azimuth ->
            filteredAzimuth = if (isFirstReading) {
                isFirstReading = false
                azimuth
            } else {
                QiblaCalculator.smoothAngle(filteredAzimuth, azimuth, smoothingAlpha)
            }
            onOrientationChanged?.invoke(filteredAzimuth, event.accuracy)
        }
    }

    private fun computeAzimuthFromGravityAndMagnetic(): Float? {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            return ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).toFloat()
        }
        return null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Accuracy notifications delivered with next onSensorChanged event
    }
}
