package com.bridgeconn.soundalert

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.bridgeconn.soundalert.audio.ContextScorer
import com.bridgeconn.soundalert.audio.LabelScore
import kotlin.math.abs
import kotlin.math.sqrt

class ContextDetector(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val scorer = ContextScorer()

    @Volatile private var motionEma = 0f

    fun start() {
        scorer.reset()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun updateFromAudio(scores: List<LabelScore>): ContextProfile {
        val motion = (motionEma / 1.2f).coerceIn(0f, 1f)
        return scorer.update(scores, motion)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val motion = abs(magnitude - SensorManager.GRAVITY_EARTH)
        motionEma = motionEma * 0.92f + motion * 0.08f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
