package com.bridgeconn.soundalert.audio

import com.bridgeconn.soundalert.AlertKind
import com.bridgeconn.soundalert.ContextProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetectionPolicyTest {
    private val policy = DetectionPolicy()

    @Test
    fun speechAndMusicDoNotAlert() {
        val result = policy.evaluate(
            listOf(LabelScore("Speech", 0.88f), LabelScore("Music", 0.71f)),
            0.70f,
            ContextProfile.UNCERTAIN,
            0f
        )
        assertNull(result)
    }

    @Test
    fun trafficWithoutHornDoesNotAlert() {
        val result = policy.evaluate(
            listOf(LabelScore("Traffic noise, roadway noise", 0.91f), LabelScore("Car", 0.72f)),
            0.75f,
            ContextProfile.ROAD,
            0f
        )
        assertNull(result)
    }

    @Test
    fun strongHornAlertsOnRoad() {
        val result = policy.evaluate(
            listOf(LabelScore("Vehicle horn, car horn, honking", 0.70f), LabelScore("Traffic noise, roadway noise", 0.22f)),
            0.70f,
            ContextProfile.ROAD,
            0f
        )
        assertEquals(AlertKind.HORN, result?.kind)
    }

    @Test
    fun doorbellAlertsAtHome() {
        val result = policy.evaluate(
            listOf(LabelScore("Doorbell", 0.67f), LabelScore("Inside, small room", 0.61f)),
            0.70f,
            ContextProfile.HOME,
            0f
        )
        assertEquals(AlertKind.DOOR, result?.kind)
    }


    @Test
    fun requestedNegativeBackgroundClassesDoNotAlert() {
        val backgrounds = listOf(
            "Speech",
            "Music",
            "Engine",
            "Wind",
            "Jackhammer",
            "Traffic noise, roadway noise",
            "Television",
            "Dog",
            "Dishes, pots, and pans"
        )
        backgrounds.forEach { label ->
            val result = policy.evaluate(
                listOf(LabelScore(label, 0.95f)),
                0.90f,
                ContextProfile.UNCERTAIN,
                0f
            )
            assertNull("Unexpected alert for negative/background class: $label", result)
        }
    }

    @Test
    fun sirenHasItsOwnAlertKind() {
        val result = policy.evaluate(
            listOf(LabelScore("Ambulance (siren)", 0.72f)),
            0.70f,
            ContextProfile.ROAD,
            0f
        )
        assertEquals(AlertKind.SIREN, result?.kind)
    }

    @Test
    fun resetClearsPendingFrames() {
        val gate = MultiFrameGate()
        val horn = DetectionPolicy.Candidate(AlertKind.HORN, 0.75f)
        assertNull(gate.push(horn))
        gate.reset()
        assertNull(gate.push(horn))
    }

    @Test
    fun gateNeedsTwoOfThreeFrames() {
        val gate = MultiFrameGate()
        val horn = DetectionPolicy.Candidate(AlertKind.HORN, 0.7f)
        assertNull(gate.push(horn))
        assertNull(gate.push(null))
        assertEquals(AlertKind.HORN, gate.push(horn)?.kind)
    }
}
