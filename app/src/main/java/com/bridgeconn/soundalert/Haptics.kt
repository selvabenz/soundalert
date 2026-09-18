package com.bridgeconn.soundalert

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    // Waveforms are [off, on, off, on...]. They intentionally differ from common call/message buzzes.
    private val hornPattern = longArrayOf(0, 700, 170, 190, 170, 700)
    private val doorPattern = longArrayOf(0, 140, 100, 140, 480, 140, 100, 140)

    fun vibrate(context: Context, mode: AlertMode) {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return

        val timings = if (mode == AlertMode.ROAD) hornPattern else doorPattern
        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
    }
}
