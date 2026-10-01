package com.example.popmind.session

import android.os.SystemClock
import com.example.popmind.data.local.ActiveFocusEntity

data class FocusSessionState(
    val isActive: Boolean = false,
    val task: String = "",
    val pomodoro: Boolean = true,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val phase: String = PHASE_FOCUS,
    val startedAtWall: Long = 0,
    val startedAtElapsed: Long = 0,
    val phaseEndElapsed: Long = 0,
    val interruptions: Int = 0,
    val completedCycles: Int = 0,
    val sound: String = SOUND_OFF,
    val soundEnabled: Boolean = false,
    val volume: Float = .35f,
    val pinRequested: Boolean = false,
    val tryDnd: Boolean = false,
    val oldInterruptionFilter: Int = 1,
    val changedDnd: Boolean = false,
    val sessionId: Long = -1,
    val tick: Long = 0,
    val ended: Boolean = false,
    val completedResult: Boolean = true
) {
    fun secondsRemaining(now: Long = SystemClock.elapsedRealtime()): Long =
        if (!isActive) 0 else if (!pomodoro) (now - startedAtElapsed).coerceAtLeast(0) / 1000 else (phaseEndElapsed - now).coerceAtLeast(0) / 1000

    fun elapsedSeconds(now: Long = SystemClock.elapsedRealtime()): Long = (now - startedAtElapsed).coerceAtLeast(0) / 1000

    fun toEntity() = ActiveFocusEntity(
        task = task, pomodoro = pomodoro, focusMinutes = focusMinutes, breakMinutes = breakMinutes,
        phase = phase, startedAtWall = startedAtWall, startedAtElapsed = startedAtElapsed,
        phaseEndElapsed = phaseEndElapsed, interruptions = interruptions, completedCycles = completedCycles,
        sound = sound, soundEnabled = soundEnabled, volume = volume, pinRequested = pinRequested, tryDnd = tryDnd,
        oldInterruptionFilter = oldInterruptionFilter, changedDnd = changedDnd, sessionId = sessionId
    )

    companion object {
        const val PHASE_FOCUS = "focus"
        const val PHASE_BREAK = "break"
        const val SOUND_OFF = "off"
        const val SOUND_RAIN = "rain"
        const val SOUND_LOFI = "lofi"
        const val SOUND_WHITE = "white"

        fun fromEntity(entity: ActiveFocusEntity) = FocusSessionState(
            isActive = true, task = entity.task, pomodoro = entity.pomodoro,
            focusMinutes = entity.focusMinutes, breakMinutes = entity.breakMinutes, phase = entity.phase,
            startedAtWall = entity.startedAtWall, startedAtElapsed = entity.startedAtElapsed,
            phaseEndElapsed = entity.phaseEndElapsed, interruptions = entity.interruptions,
            completedCycles = entity.completedCycles, sound = entity.sound, soundEnabled = entity.soundEnabled,
            volume = entity.volume, pinRequested = entity.pinRequested, tryDnd = entity.tryDnd, oldInterruptionFilter = entity.oldInterruptionFilter,
            changedDnd = entity.changedDnd, sessionId = entity.sessionId
        )
    }
}
