package com.bridgeconn.soundalert.audio

import com.bridgeconn.soundalert.AlertMode
import com.bridgeconn.soundalert.SoundLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class HeuristicSoundClassifierTest {
    private val sampleRate = 16_000
    private val frameSize = 2048

    @Test
    fun silenceDoesNotAlert() {
        val classifier = HeuristicSoundClassifier(sampleRate)
        val silence = ShortArray(frameSize)
        assertNull(classifier.analyze(silence, silence.size, AlertMode.ROAD, 0.70f))
    }

    @Test
    fun sustained440HzToneLooksHornLike() {
        val classifier = HeuristicSoundClassifier(sampleRate)
        val frame = sine(440.0, 0.55)
        classifier.analyze(frame, frame.size, AlertMode.ROAD, 0.70f)
        val result = classifier.analyze(frame, frame.size, AlertMode.ROAD, 0.70f)
        assertNotNull(result)
        assertEquals(SoundLabel.HORN, result?.label)
    }

    @Test
    fun sustained1200HzToneLooksDoorbellLike() {
        val classifier = HeuristicSoundClassifier(sampleRate)
        val frame = sine(1200.0, 0.50)
        classifier.analyze(frame, frame.size, AlertMode.HOME, 0.70f)
        val result = classifier.analyze(frame, frame.size, AlertMode.HOME, 0.70f)
        assertNotNull(result)
        assertEquals(SoundLabel.DOORBELL_OR_KNOCK, result?.label)
    }

    private fun sine(frequency: Double, amplitude: Double): ShortArray =
        ShortArray(frameSize) { i ->
            (sin(2.0 * PI * frequency * i / sampleRate) * amplitude * Short.MAX_VALUE).toInt().toShort()
        }
}
