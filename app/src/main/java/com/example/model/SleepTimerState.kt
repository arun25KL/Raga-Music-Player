package com.example.model

enum class SleepTimerOption(val title: String, val minutes: Int) {
    OFF("Off", 0),
    MIN_15("15 minutes", 15),
    MIN_30("30 minutes", 30),
    MIN_45("45 minutes", 45),
    MIN_60("60 minutes", 60),
    END_OF_TRACK("End of current track", -1)
}

data class SleepTimerState(
    val selectedOption: SleepTimerOption = SleepTimerOption.OFF,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false
) {
    val remainingFormatted: String
        get() {
            if (!isRunning) return "Off"
            if (selectedOption == SleepTimerOption.END_OF_TRACK) return "End of Track"
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}
