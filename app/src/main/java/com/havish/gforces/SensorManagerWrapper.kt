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
import kotlin.math.cos
import kotlin.math.sin

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
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val prefs = PreferencesManager(context)

    // Low-pass filter variables
    private val alpha = 0.85f // alpha for low-pass filter (gravity adapts slowly)
    private var lpGravityX = 0f
    private var lpGravityY = 0f
    private var lpGravityZ = SensorManager.GRAVITY_EARTH // Default to 1G on Z

    // Fused gravity vector
    private var fusedGravityX = 0f
    private var fusedGravityY = 0f
    private var fusedGravityZ = SensorManager.GRAVITY_EARTH

    // Latest orientation derived gravity
    private var derivedGravX = 0f
    private var derivedGravY = 0f
    private var derivedGravZ = SensorManager.GRAVITY_EARTH

    private var hasOrientation = false

    fun calibrate() {
        prefs.calibX = fusedGravityX
        prefs.calibY = fusedGravityY
        prefs.calibZ = fusedGravityZ
    }

    fun getGForceData(): Flow<GForceData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        
                        // pitch (beta) and roll (gamma)
                        val beta = orientation[1]
                        val gamma = orientation[2]
                        val G = SensorManager.GRAVITY_EARTH

                        // Calculate gravity vector from orientation
                        // Note: Android accelerometer reports +G on Z when face up.
                        // We adjust the signs to match Android's coordinate system where the gravity vector
                        // (as reported by accelerometer at rest) points up.
                        derivedGravX = G * sin(gamma) * cos(beta)
                        derivedGravY = -G * sin(beta)
                        derivedGravZ = G * cos(gamma) * cos(beta)
                        hasOrientation = true
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val accelX = event.values[0]
                        val accelY = event.values[1]
                        val accelZ = event.values[2]

                        // Method 1: Low-Pass Filter
                        lpGravityX = alpha * lpGravityX + (1 - alpha) * accelX
                        lpGravityY = alpha * lpGravityY + (1 - alpha) * accelY
                        lpGravityZ = alpha * lpGravityZ + (1 - alpha) * accelZ

                        // Method 2: Sensor Fusion (Blend)
                        if (hasOrientation) {
                            fusedGravityX = 0.95f * derivedGravX + 0.05f * lpGravityX
                            fusedGravityY = 0.95f * derivedGravY + 0.05f * lpGravityY
                            fusedGravityZ = 0.95f * derivedGravZ + 0.05f * lpGravityZ
                        } else {
                            fusedGravityX = lpGravityX
                            fusedGravityY = lpGravityY
                            fusedGravityZ = lpGravityZ
                        }

                        // Calculate dynamic acceleration by subtracting fused gravity
                        val dynX = accelX - fusedGravityX
                        val dynY = accelY - fusedGravityY
                        val dynZ = accelZ - fusedGravityZ

                        val cx = prefs.calibX
                        val cy = prefs.calibY
                        val cz = prefs.calibZ

                        // Using calibration just to determine phone placement orientation, 
                        // though we could also apply calibration offset to dynamic vector.
                        val diffX = dynX / SensorManager.GRAVITY_EARTH
                        val diffY = dynY / SensorManager.GRAVITY_EARTH
                        val diffZ = dynZ / SensorManager.GRAVITY_EARTH

                        val lateralG: Float
                        val longitudinalG: Float

                        if (abs(cy) > abs(cz)) {
                            // Phone is upright
                            lateralG = diffX
                            longitudinalG = diffZ
                        } else {
                            // Phone is flat
                            lateralG = diffX
                            longitudinalG = diffY
                        }

                        trySend(
                            GForceData(
                                lateralG = lateralG,
                                longitudinalG = longitudinalG,
                                rawX = dynX, // Now exposing dynamic Gs instead of raw for visualization
                                rawY = dynY,
                                rawZ = dynZ
                            )
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        if (rotationVector != null) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
