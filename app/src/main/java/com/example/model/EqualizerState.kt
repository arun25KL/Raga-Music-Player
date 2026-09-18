package com.example.model

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int, // MilliBels (-1500 to +1500 typically)
    val minMb: Int = -1500,
    val maxMb: Int = 1500
) {
    val displayFrequency: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000} kHz"
        } else {
            "$centerFreqHz Hz"
        }
}

enum class EqualizerPreset(val title: String) {
    FLAT("Flat"),
    BASS_BOOST("Bass Boost"),
    ROCK("Rock"),
    POP("Pop"),
    VOCAL("Vocal"),
    CLASSICAL("Classical"),
    CUSTOM("Custom")
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val currentPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val bassBoostStrength: Int = 0, // 0 to 1000 (0% to 100%)
    val virtualizerStrength: Int = 0, // 0 to 1000 (0% to 100% 3D Surround)
    val bands: List<EqualizerBand> = emptyList()
)

data class AbLoopState(
    val pointA: Long? = null,
    val pointB: Long? = null,
    val isEnabled: Boolean = false
) {
    val isReadyToLoop: Boolean
        get() = pointA != null && pointB != null && pointB > pointA && isEnabled
}
