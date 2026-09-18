package com.bridgeconn.soundalert.audio

import com.bridgeconn.soundalert.AlertKind
import com.bridgeconn.soundalert.ContextProfile

class DetectionPolicy {
    data class Candidate(val kind: AlertKind, val score: Float)

    fun evaluate(
        scores: List<LabelScore>,
        sensitivity: Float,
        profile: ContextProfile,
        calibrationAdjustment: Float
    ): Candidate? {
        if (scores.isEmpty()) return null
        val byName = scores.associate { it.label to it.score }

        val horn = maxOf(byName, HORN_LABELS)
        val siren = maxOf(byName, SIREN_LABELS)
        val doorbell = maxOf(byName, DOORBELL_LABELS)
        val knock = maxOf(byName, KNOCK_LABELS)
        val roadSignal = maxOf(horn, maxOf(byName, BICYCLE_LABELS))
        val homeSignal = maxOf(doorbell, knock)
        val negative = backgroundScore(scores)

        val base = sensitivityThreshold(sensitivity) + calibrationAdjustment
        val roadThreshold = (base + if (profile == ContextProfile.ROAD) -0.045f else if (profile == ContextProfile.HOME) 0.055f else 0f)
            .coerceIn(0.24f, 0.66f)
        val homeThreshold = (base + if (profile == ContextProfile.HOME) -0.045f else if (profile == ContextProfile.ROAD) 0.055f else 0f)
            .coerceIn(0.24f, 0.66f)
        val sirenThreshold = (base - 0.025f).coerceIn(0.23f, 0.62f)

        val hornAdjusted = suppress(roadSignal, negative)
        val doorAdjusted = suppress(homeSignal, negative)
        val sirenAdjusted = suppress(siren, negative * 0.65f)

        return when {
            sirenAdjusted >= sirenThreshold -> Candidate(AlertKind.SIREN, sirenAdjusted)
            hornAdjusted >= roadThreshold -> Candidate(AlertKind.HORN, hornAdjusted)
            doorAdjusted >= homeThreshold -> Candidate(AlertKind.DOOR, doorAdjusted)
            else -> null
        }
    }

    fun backgroundScore(scores: List<LabelScore>): Float {
        var best = 0f
        for (score in scores) {
            if (score.label in NEGATIVE_BACKGROUND && score.score > best) best = score.score
        }
        return best.coerceIn(0f, 1f)
    }

    fun maxRelevantScore(scores: List<LabelScore>): Float {
        val map = scores.associate { it.label to it.score }
        return maxOf(
            maxOf(map, HORN_LABELS),
            maxOf(map, SIREN_LABELS),
            maxOf(map, DOORBELL_LABELS),
            maxOf(map, KNOCK_LABELS),
            maxOf(map, BICYCLE_LABELS)
        )
    }

    private fun sensitivityThreshold(sensitivity: Float): Float {
        val normalized = ((sensitivity.coerceIn(0.30f, 0.95f) - 0.30f) / 0.65f)
        return 0.52f - normalized * 0.22f
    }

    private fun suppress(positive: Float, negative: Float): Float {
        if (positive >= 0.86f) return positive
        val penalty = when {
            negative < 0.18f -> 0f
            negative < 0.40f -> negative * 0.18f
            else -> negative * 0.32f
        }
        return (positive - penalty).coerceIn(0f, 1f)
    }

    private fun maxOf(map: Map<String, Float>, labels: Set<String>): Float =
        labels.maxOfOrNull { map[it] ?: 0f } ?: 0f

    private fun maxOf(vararg values: Float): Float = values.maxOrNull() ?: 0f

    companion object {
        val HORN_LABELS = setOf(
            "Vehicle horn, car horn, honking",
            "Toot",
            "Air horn, truck horn"
        )

        val SIREN_LABELS = setOf(
            "Emergency vehicle",
            "Police car (siren)",
            "Ambulance (siren)",
            "Fire engine, fire truck (siren)",
            "Siren"
        )

        val BICYCLE_LABELS = setOf("Bicycle bell")
        val DOORBELL_LABELS = setOf("Doorbell", "Ding-dong")
        val KNOCK_LABELS = setOf("Knock")

        // Explicit negatives requested for v0.2.0, expanded with closely related AudioSet classes.
        val NEGATIVE_BACKGROUND = setOf(
            "Speech", "Child speech, kid speaking", "Conversation", "Narration, monologue",
            "Shout", "Yell", "Children shouting", "Screaming", "Chatter", "Crowd",
            "Hubbub, speech noise, speech babble",
            "Music", "Background music", "Song", "Radio",
            "Engine", "Light engine (high frequency)", "Medium engine (mid frequency)",
            "Heavy engine (low frequency)", "Engine knocking", "Engine starting", "Idling",
            "Accelerating, revving, vroom",
            "Wind", "Wind noise (microphone)", "Rain", "Thunderstorm",
            "Jackhammer", "Hammer", "Drill", "Power tool", "Sawing", "Sanding",
            "Traffic noise, roadway noise", "Car passing by",
            "Television",
            "Dog", "Bark", "Howl", "Growling",
            "Dishes, pots, and pans", "Cutlery, silverware", "Chopping (food)", "Frying (food)",
            "Microwave oven", "Blender", "Water tap, faucet", "Vacuum cleaner",
            "Mechanical fan", "Air conditioning",
            "Noise", "Environmental noise", "White noise", "Pink noise", "Cacophony"
        )
    }
}
