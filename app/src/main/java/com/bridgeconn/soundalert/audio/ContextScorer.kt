package com.bridgeconn.soundalert.audio

import com.bridgeconn.soundalert.ContextProfile

class ContextScorer {
    private var roadEma = 0f
    private var homeEma = 0f

    fun update(scores: List<LabelScore>, motion: Float): ContextProfile {
        val byName = scores.associate { it.label to it.score }
        val roadAudio = maxOfNames(byName, ROAD_CONTEXT)
        val homeAudio = maxOfNames(byName, HOME_CONTEXT)
        val motionScore = motion.coerceIn(0f, 1f)

        val roadNow = (roadAudio * 0.78f + motionScore * 0.22f).coerceIn(0f, 1f)
        val homeNow = (homeAudio * 0.84f + (1f - motionScore) * 0.16f).coerceIn(0f, 1f)

        roadEma = roadEma * 0.88f + roadNow * 0.12f
        homeEma = homeEma * 0.88f + homeNow * 0.12f

        return when {
            roadEma > 0.26f && roadEma > homeEma + 0.09f -> ContextProfile.ROAD
            homeEma > 0.24f && homeEma > roadEma + 0.09f -> ContextProfile.HOME
            else -> ContextProfile.UNCERTAIN
        }
    }

    fun reset() {
        roadEma = 0f
        homeEma = 0f
    }

    private fun maxOfNames(map: Map<String, Float>, names: Set<String>): Float =
        names.maxOfOrNull { map[it] ?: 0f } ?: 0f

    companion object {
        val ROAD_CONTEXT = setOf(
            "Traffic noise, roadway noise",
            "Motor vehicle (road)",
            "Vehicle",
            "Car",
            "Truck",
            "Bus",
            "Motorcycle",
            "Car passing by",
            "Outside, urban or manmade"
        )

        val HOME_CONTEXT = setOf(
            "Inside, small room",
            "Inside, large room or hall",
            "Door",
            "Dishes, pots, and pans",
            "Television",
            "Mechanical fan",
            "Air conditioning"
        )
    }
}
