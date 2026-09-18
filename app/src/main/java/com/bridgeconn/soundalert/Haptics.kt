package com.bridgeconn.soundalert

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    private val horn = longArrayOf(0, 650, 140, 180, 140, 650)
    private val door = longArrayOf(0, 130, 90, 130, 420, 130, 90, 130)
    private val siren = longArrayOf(0, 360, 100, 360, 100, 360, 420, 360, 100, 360, 100, 360)

    fun duration(kind: AlertKind): Long = when (kind) {
        AlertKind.HORN -> horn.sum()
        AlertKind.DOOR -> door.sum()
        AlertKind.SIREN -> siren.sum()
        AlertKind.NONE -> 0L
    }

    fun vibrate(context: Context, kind: AlertKind) {
        if (kind == AlertKind.NONE) return
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        val pattern = when (kind) {
            AlertKind.HORN -> horn
            AlertKind.DOOR -> door
            AlertKind.SIREN -> siren
            AlertKind.NONE -> return
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
