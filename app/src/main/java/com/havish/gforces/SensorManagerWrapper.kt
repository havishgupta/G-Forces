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
    // Accelerometer is much better for G-Force apps as it measures real acceleration (movement) + gravity.
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val prefs = PreferencesManager(context)

    // Low-pass filter variables to smooth out jitter
    private val alpha = 0.15f
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    fun calibrate() {
        prefs.calibX = gravityX
        prefs.calibY = gravityY
        prefs.calibZ = gravityZ
    }

    fun getGForceData(): Flow<GForceData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Apply low-pass filter
                    gravityX = alpha * it.values[0] + (1 - alpha) * gravityX
                    gravityY = alpha * it.values[1] + (1 - alpha) * gravityY
                    gravityZ = alpha * it.values[2] + (1 - alpha) * gravityZ

                    val cx = prefs.calibX
                    val cy = prefs.calibY
                    val cz = prefs.calibZ

                    // Remove baseline gravity
                    val diffX = (gravityX - cx) / SensorManager.GRAVITY_EARTH
                    val diffY = (gravityY - cy) / SensorManager.GRAVITY_EARTH
                    val diffZ = (gravityZ - cz) / SensorManager.GRAVITY_EARTH

                    // Heuristic: Determine phone orientation based on calibration vector
                    // If |cy| > |cz|, phone is mostly upright. Horizontal plane is X and Z.
                    // If |cz| > |cy|, phone is mostly flat. Horizontal plane is X and Y.
                    val lateralG: Float
                    val longitudinalG: Float

                    if (abs(cy) > abs(cz)) {
                        // Phone is upright (portrait or landscape, but screen facing driver)
                        lateralG = diffX
                        longitudinalG = diffZ // Z is forward/backward
                    } else {
                        // Phone is flat on a surface (screen facing sky)
                        lateralG = diffX
                        longitudinalG = diffY // Y is forward/backward
                    }

                    trySend(
                        GForceData(
                            lateralG = lateralG,
                            longitudinalG = longitudinalG,
                            rawX = gravityX,
                            rawY = gravityY,
                            rawZ = gravityZ
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            // SENSOR_DELAY_GAME provides a very smooth ~50fps refresh rate
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
