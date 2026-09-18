package com.bridgeconn.soundalert

import android.content.Context
import kotlin.math.max

class CalibrationManager(context: Context) {
    private val prefs = context.getSharedPreferences("soundalert_calibration", Context.MODE_PRIVATE)
    private var roadBackground = prefs.getFloat("road_background", 0.15f)
    private var homeBackground = prefs.getFloat("home_background", 0.10f)
    private var uncertainBackground = prefs.getFloat("uncertain_background", 0.12f)
    private var updateCount = 0

    fun observe(profile: ContextProfile, backgroundScore: Float) {
        val bounded = backgroundScore.coerceIn(0f, 1f)
        when (profile) {
            ContextProfile.ROAD -> roadBackground = ema(roadBackground, bounded)
            ContextProfile.HOME -> homeBackground = ema(homeBackground, bounded)
            ContextProfile.UNCERTAIN -> uncertainBackground = ema(uncertainBackground, bounded)
        }
        updateCount++
        if (updateCount % 60 == 0) {
            prefs.edit()
                .putFloat("road_background", roadBackground)
                .putFloat("home_background", homeBackground)
                .putFloat("uncertain_background", uncertainBackground)
                .apply()
        }
    }

    fun thresholdAdjustment(profile: ContextProfile): Float {
        val baseline = when (profile) {
            ContextProfile.ROAD -> roadBackground
            ContextProfile.HOME -> homeBackground
            ContextProfile.UNCERTAIN -> uncertainBackground
        }
        return (max(0f, baseline - 0.18f) * 0.18f).coerceIn(0f, 0.08f)
    }

    private fun ema(old: Float, new: Float): Float = old * 0.985f + new * 0.015f
}
