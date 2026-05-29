package com.havish.gforces

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

data class GForceData(
    val lateralG: Float,
    val longitudinalG: Float,
    val rawX: Float,
    val rawY: Float,
    val rawZ: Float
)

class SensorManagerWrapper(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val linearAcceleration: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    val prefs = PreferencesManager(context)

    // Low-pass filter variables for gravity tracking (always running for orientation)
    private val alpha = 0.85f
    private var lpGravityX = 0f
    private var lpGravityY = 0f
    private var lpGravityZ = SensorManager.GRAVITY_EARTH

    fun calibrate() {
        prefs.calibX = lpGravityX
        prefs.calibY = lpGravityY
        prefs.calibZ = lpGravityZ
    }

    fun getGForceData(): Flow<GForceData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val isTrueGMode = prefs.useTrueGForceMode

                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val accelX = event.values[0]
                    val accelY = event.values[1]
                    val accelZ = event.values[2]

                    // Always update low-pass gravity to track device orientation
                    lpGravityX = alpha * lpGravityX + (1 - alpha) * accelX
                    lpGravityY = alpha * lpGravityY + (1 - alpha) * accelY
                    lpGravityZ = alpha * lpGravityZ + (1 - alpha) * accelZ

                    if (!isTrueGMode) {
                        // Regular mode: unassisted raw acceleration (smoothed via low-pass) minus static calibration
                        val dynX = lpGravityX - prefs.calibX
                        val dynY = lpGravityY - prefs.calibY
                        val dynZ = lpGravityZ - prefs.calibZ

                        val diffX = dynX / SensorManager.GRAVITY_EARTH
                        val diffY = dynY / SensorManager.GRAVITY_EARTH
                        val diffZ = dynZ / SensorManager.GRAVITY_EARTH

                        val lateralG: Float
                        val longitudinalG: Float

                        // Determine orientation based on calibration or current gravity
                        val cy = prefs.calibY.takeIf { it != 0f } ?: lpGravityY
                        val cz = prefs.calibZ.takeIf { it != 0f } ?: lpGravityZ

                        if (abs(cy) > abs(cz)) {
                            lateralG = diffX
                            longitudinalG = diffZ
                        } else {
                            lateralG = diffX
                            longitudinalG = diffY
                        }

                        trySend(GForceData(lateralG, longitudinalG, lpGravityX, lpGravityY, lpGravityZ))
                    }
                } else if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                    if (isTrueGMode) {
                        val dynX = event.values[0]
                        val dynY = event.values[1]
                        val dynZ = event.values[2]

                        val diffX = dynX / SensorManager.GRAVITY_EARTH
                        val diffY = dynY / SensorManager.GRAVITY_EARTH
                        val diffZ = dynZ / SensorManager.GRAVITY_EARTH

                        val lateralG: Float
                        val longitudinalG: Float

                        // Determine orientation based on calibration or current gravity
                        val cy = prefs.calibY.takeIf { it != 0f } ?: lpGravityY
                        val cz = prefs.calibZ.takeIf { it != 0f } ?: lpGravityZ

                        if (abs(cy) > abs(cz)) {
                            lateralG = diffX
                            longitudinalG = diffZ
                        } else {
                            lateralG = diffX
                            longitudinalG = diffY
                        }

                        trySend(GForceData(lateralG, longitudinalG, dynX, dynY, dynZ))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        if (linearAcceleration != null) {
            sensorManager.registerListener(listener, linearAcceleration, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
