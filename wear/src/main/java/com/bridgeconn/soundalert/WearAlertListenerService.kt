package com.bridgeconn.soundalert

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearAlertListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != PATH) return
        val kind = messageEvent.data.toString(Charsets.UTF_8)
        val pattern = when (kind) {
            "HORN" -> longArrayOf(0, 650, 140, 180, 140, 650)
            "DOOR" -> longArrayOf(0, 130, 90, 130, 420, 130, 90, 130)
            "SIREN" -> longArrayOf(0, 360, 100, 360, 100, 360, 420, 360, 100, 360, 100, 360)
            else -> return
        }
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    companion object {
        private const val PATH = "/soundalert/alert"
    }
}
