package com.havish.gforces

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class GForceData(val x: Float, val y: Float, val z: Float)

class SensorManagerWrapper(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // The user specifically requested "Gravity (Non-Wakeup)"
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    fun getGravityData(): Flow<GForceData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Convert from m/s^2 to G's
                    val xGs = it.values[0] / SensorManager.GRAVITY_EARTH
                    val yGs = it.values[1] / SensorManager.GRAVITY_EARTH
                    val zGs = it.values[2] / SensorManager.GRAVITY_EARTH
                    trySend(GForceData(xGs, yGs, zGs))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (gravitySensor != null) {
            sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
