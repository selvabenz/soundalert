package com.bridgeconn.soundalert.audio

import com.bridgeconn.soundalert.ContextProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextScorerTest {
    @Test
    fun learnsRoadContextFromTrafficAndMotion() {
        val scorer = ContextScorer()
        var result = ContextProfile.UNCERTAIN
        repeat(12) {
            result = scorer.update(
                listOf(LabelScore("Traffic noise, roadway noise", 0.8f), LabelScore("Motor vehicle (road)", 0.65f)),
                0.75f
            )
        }
        assertEquals(ContextProfile.ROAD, result)
    }

    @Test
    fun learnsHomeContextFromIndoorAudioAndStillness() {
        val scorer = ContextScorer()
        var result = ContextProfile.UNCERTAIN
        repeat(12) {
            result = scorer.update(
                listOf(LabelScore("Inside, small room", 0.78f), LabelScore("Mechanical fan", 0.45f)),
                0.05f
            )
        }
        assertEquals(ContextProfile.HOME, result)
    }
}
