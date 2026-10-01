package com.example.popmind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.NotificationManager as SystemNotificationManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.popmind.MainActivity
import com.example.popmind.R
import com.example.popmind.data.local.PopMindDatabase
import com.example.popmind.data.local.SessionEntity
import com.example.popmind.session.FocusSessionRepository
import com.example.popmind.session.FocusSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusSessionService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var state = FocusSessionState()
    private var sessionWriteJob: Job? = null
    private var lastCheckpoint = 0L
    private var lastNotificationMinute = -1L
    private var lastNotificationPhase = ""
    private val appLeftReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { remindToReturn() }
    }

    override fun onCreate() {
        super.onCreate()
        FocusSessionRepository.initialize(this)
        createChannels()
        audioManager = getSystemService(AudioManager::class.java)
        val filter = IntentFilter(ACTION_APP_LEFT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(appLeftReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(appLeftReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(intent)
            ACTION_RESTORE, null -> restoreSession()
            ACTION_END -> finishSession(completed = true)
            ACTION_ABANDON -> finishSession(completed = false)
            ACTION_INTERRUPT -> recordInterruption()
            ACTION_APP_LEFT -> if (state.isActive) remindToReturn() else restoreSession()
            ACTION_TOGGLE_MUSIC -> toggleMusic()
            ACTION_SET_VOLUME -> setVolume(intent.getFloatExtra(EXTRA_VOLUME, state.volume))
            else -> if (state.isActive) startTicker() else stopSelf()
        }
        return START_STICKY
    }

    private fun startSession(intent: Intent) {
        if (state.isActive) {
            restoreSession()
            return
        }
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val pomodoro = intent.getBooleanExtra(EXTRA_POMODORO, true)
        val focusMinutes = intent.getIntExtra(EXTRA_FOCUS_MINUTES, 25).coerceIn(5, 120)
        val breakMinutes = intent.getIntExtra(EXTRA_BREAK_MINUTES, 5).coerceIn(5, 10)
        val sound = intent.getStringExtra(EXTRA_SOUND) ?: FocusSessionState.SOUND_OFF
        val soundEnabled = sound != FocusSessionState.SOUND_OFF
        val manager = getSystemService(SystemNotificationManager::class.java)
        val tryDnd = intent.getBooleanExtra(EXTRA_TRY_DND, false) && manager.isNotificationPolicyAccessGranted
        val oldFilter = if (tryDnd) manager.currentInterruptionFilter else SystemNotificationManager.INTERRUPTION_FILTER_ALL
        state = FocusSessionState(
            isActive = true,
            task = intent.getStringExtra(EXTRA_TASK).orEmpty().ifBlank { "Phiên tập trung" },
            pomodoro = pomodoro, focusMinutes = focusMinutes, breakMinutes = breakMinutes,
            phase = FocusSessionState.PHASE_FOCUS, startedAtWall = nowWall, startedAtElapsed = nowElapsed,
            phaseEndElapsed = nowElapsed + phaseDuration(true, focusMinutes, breakMinutes),
            sound = sound, soundEnabled = soundEnabled,
            volume = intent.getFloatExtra(EXTRA_VOLUME, .35f).coerceIn(0f, 1f),
            pinRequested = intent.getBooleanExtra(EXTRA_PIN, false), tryDnd = tryDnd,
            oldInterruptionFilter = oldFilter, changedDnd = tryDnd
        )
        if (tryDnd) runCatching { manager.setInterruptionFilter(SystemNotificationManager.INTERRUPTION_FILTER_NONE) }
        startForegroundCompat(buildNotification())
        acquireWakeLock()
        publish(persist = true)
        // Lưu phiên và trạng thái phục hồi trước khi đồng hồ bắt đầu chạy.
        sessionWriteJob = io.launch {
            val dao = PopMindDatabase.getInstance(applicationContext).sessionDao()
            val id = dao.insertSession(SessionEntity(
                startTime = nowWall, durationSeconds = 0L, task = state.task, interruptions = 0,
                usedPomodoro = pomodoro, completed = false
            ))
            state = state.copy(sessionId = id)
            FocusSessionRepository.publish(state, persist = true)
        }
        if (soundEnabled) startMusic()
        startTicker()
    }

    private fun restoreSession() {
        if (state.isActive) {
            startForegroundCompat(buildNotification())
            acquireWakeLock()
            if (state.soundEnabled) startMusic()
            startTicker()
            return
        }
        // Lệnh sticky có thể đến trước Activity; lên foreground trước khi đọc Room.
        startForegroundCompat(buildRestoringNotification())
        io.launch {
            val stored = PopMindDatabase.getInstance(applicationContext).activeFocusDao().getActive()
            if (stored == null) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            var restored = FocusSessionState.fromEntity(stored)
            val nowElapsed = SystemClock.elapsedRealtime()
            // Sau khi thiết bị khởi động lại elapsedRealtime bắt đầu lại, nên mở giai đoạn an toàn mới.
            if (restored.startedAtElapsed > nowElapsed || restored.phaseEndElapsed < 0L) {
                restored = restored.copy(startedAtElapsed = nowElapsed, phaseEndElapsed = nowElapsed + phaseDuration(true, restored.focusMinutes, restored.breakMinutes), phase = FocusSessionState.PHASE_FOCUS)
            }
            state = restored
            lastNotificationMinute = -1L
            lastNotificationPhase = ""
            FocusSessionRepository.publish(state, persist = true)
            if (state.changedDnd) runCatching { getSystemService(SystemNotificationManager::class.java).setInterruptionFilter(SystemNotificationManager.INTERRUPTION_FILTER_NONE) }
            startForegroundCompat(buildNotification())
            acquireWakeLock()
            if (state.soundEnabled) startMusic()
            startTicker()
        }
    }

    private fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!state.isActive) return
            advancePhases()
            state = state.copy(tick = SystemClock.elapsedRealtime())
            FocusSessionRepository.publish(state)
            val now = SystemClock.elapsedRealtime()
            if (now - lastCheckpoint >= 60_000L) {
                lastCheckpoint = now
                persistCheckpoint()
            }
            val minute = state.secondsRemaining() / 60
            if (minute != lastNotificationMinute || state.phase != lastNotificationPhase) {
                lastNotificationMinute = minute
                lastNotificationPhase = state.phase
                updateNotification()
            }
            handler.postDelayed(this, 1_000L)
        }
    }

    private fun advancePhases() {
        if (!state.pomodoro) return
        val now = SystemClock.elapsedRealtime()
        var next = state
        var transitions = 0
        while (next.phaseEndElapsed <= now && transitions < 100) {
            if (next.phase == FocusSessionState.PHASE_FOCUS) {
                next = next.copy(phase = FocusSessionState.PHASE_BREAK, completedCycles = next.completedCycles + 1,
                    phaseEndElapsed = next.phaseEndElapsed + phaseDuration(false, next.focusMinutes, next.breakMinutes))
            } else {
                next = next.copy(phase = FocusSessionState.PHASE_FOCUS,
                    phaseEndElapsed = next.phaseEndElapsed + phaseDuration(true, next.focusMinutes, next.breakMinutes))
            }
            transitions++
        }
        if (next.phase != state.phase) {
            state = next
            publish(persist = true)
        }
    }

    private fun recordInterruption() {
        if (!state.isActive || state.phase != FocusSessionState.PHASE_FOCUS) return
        state = state.copy(interruptions = state.interruptions + 1)
        publish(persist = true)
    }

    private fun remindToReturn() {
        if (!LOCK_SOFT_ENABLED || !state.isActive || state.phase != FocusSessionState.PHASE_FOCUS) return
        recordInterruption()
        val minutes = (state.secondsRemaining() + 59) / 60
        val openApp = openSessionPendingIntent()
        val notification = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Quay lại nhé")
            .setContentText(if (state.pomodoro) "Còn $minutes phút — bạn vẫn làm được mà 🌱" else "Bạn đã tập trung $minutes phút — quay lại nhé 🌱")
            .setContentIntent(openApp).setAutoCancel(true).build()
        runCatching { getSystemService(NotificationManager::class.java).notify(REMINDER_NOTIFICATION_ID, notification) }
    }

    private fun toggleMusic() {
        if (!state.isActive) return
        state = state.copy(soundEnabled = !state.soundEnabled)
        if (state.soundEnabled && state.sound != FocusSessionState.SOUND_OFF) startMusic() else stopMusic()
        publish(persist = true)
        startForegroundCompat(buildNotification())
    }

    private fun setVolume(volume: Float) {
        state = state.copy(volume = volume.coerceIn(0f, 1f))
        player?.setVolume(state.volume, state.volume)
        publish(persist = true)
    }

    private fun startMusic() {
        if (player != null || !state.soundEnabled) return
        val resource = when (state.sound) {
            FocusSessionState.SOUND_RAIN -> R.raw.ambient_rain
            FocusSessionState.SOUND_LOFI -> R.raw.ambient_lofi
            FocusSessionState.SOUND_WHITE -> R.raw.ambient_white
            else -> return
        }
        requestAudioFocus()
        runCatching {
            MediaPlayer.create(this, resource)?.apply {
                isLooping = true
                setVolume(state.volume, state.volume)
                start()
            }
        }.onSuccess { player = it }.onFailure { stopMusic() }
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs).setOnAudioFocusChangeListener(audioFocusListener).build()
            manager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener(::onAudioFocusChanged)
    private fun onAudioFocusChanged(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> { player?.setVolume(state.volume, state.volume); if (state.soundEnabled) player?.start() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player?.setVolume(state.volume * .2f, state.volume * .2f)
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.pause()
        }
    }

    private fun stopMusic() {
        runCatching { player?.stop() }
        player?.release(); player = null
        val manager = audioManager
        if (Build.VERSION.SDK_INT >= 26) audioFocusRequest?.let { manager?.abandonAudioFocusRequest(it) }
        else {
            @Suppress("DEPRECATION")
            manager?.abandonAudioFocus(audioFocusListener)
        }
    }

    private fun finishSession(completed: Boolean) {
        if (!state.isActive) return
        val elapsed = state.elapsedSeconds()
        val finalState = state.copy(isActive = false, ended = true, completedResult = completed, tick = SystemClock.elapsedRealtime())
        state = finalState
        FocusSessionRepository.clear(finalState)
        val write = sessionWriteJob
        sessionWriteJob = io.launch {
            write?.join()
            if (state.sessionId > 0) PopMindDatabase.getInstance(applicationContext).sessionDao()
                .updateSessionResult(state.sessionId, elapsed, state.interruptions, completed)
        }
        if (state.changedDnd) restoreDnd()
        state = finalState.copy(changedDnd = false)
        FocusSessionRepository.publish(state)
        getSystemService(NotificationManager::class.java).cancel(REMINDER_NOTIFICATION_ID)
        stopMusic(); handler.removeCallbacks(ticker)
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun restoreDnd() {
        val manager = getSystemService(SystemNotificationManager::class.java)
        runCatching { if (manager.isNotificationPolicyAccessGranted) manager.setInterruptionFilter(state.oldInterruptionFilter) }
    }

    private fun persistCheckpoint() {
        FocusSessionRepository.publish(state, persist = true)
        if (state.sessionId > 0) io.launch {
            PopMindDatabase.getInstance(applicationContext).sessionDao().updateSessionResult(state.sessionId, state.elapsedSeconds(), state.interruptions, false)
        }
    }

    private fun publish(persist: Boolean = false) {
        FocusSessionRepository.publish(state, persist)
    }

    private fun phaseDuration(focus: Boolean, focusMinutes: Int, breakMinutes: Int): Long {
        if (DEBUG_SHORT_TIMER) return 60_000L
        return (if (focus) focusMinutes else breakMinutes) * 60_000L
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:focus-session").apply {
            setReferenceCounted(false); acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
    }

    private fun buildNotification(): Notification {
        val title = if (state.phase == FocusSessionState.PHASE_FOCUS) "Đang tập trung" else "Đang nghỉ"
        val clock = if (state.pomodoro) formatClock(state.secondsRemaining()) else formatClock(state.secondsRemaining())
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText("$clock · ${state.task}")
            .setContentIntent(openSessionPendingIntent()).setOngoing(true).setOnlyAlertOnce(true).build()
    }

    private fun buildRestoringNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Đang khôi phục phiên")
        .setContentText("Đồng hồ tập trung đang được khôi phục").setOngoing(true).build()

    private fun openSessionPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).setAction(ACTION_OPEN_SESSION)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, pendingFlags())
    }

    private fun updateNotification() {
        if (state.isActive) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            val mediaType = if (state.soundEnabled && state.sound != FocusSessionState.SOUND_OFF) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or mediaType)
        }
        else startForeground(NOTIFICATION_ID, notification)
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Phiên tập trung", NotificationManager.IMPORTANCE_LOW).apply { description = "Đồng hồ phiên đang chạy" })
            manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL_ID, "Nhắc quay lại", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Nhắc nhẹ khi bạn rời phiên tập trung" })
        }
    }

    private fun pendingFlags() = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    private fun formatClock(seconds: Long) = "%02d:%02d".format(seconds / 60, seconds % 60)
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        runCatching { unregisterReceiver(appLeftReceiver) }
        stopMusic(); releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.popmind.session.START"
        const val ACTION_RESTORE = "com.example.popmind.session.RESTORE"
        const val ACTION_END = "com.example.popmind.session.END"
        const val ACTION_ABANDON = "com.example.popmind.session.ABANDON"
        const val ACTION_INTERRUPT = "com.example.popmind.session.INTERRUPT"
        const val ACTION_APP_LEFT = "com.example.popmind.session.APP_LEFT"
        const val ACTION_TOGGLE_MUSIC = "com.example.popmind.session.TOGGLE_MUSIC"
        const val ACTION_SET_VOLUME = "com.example.popmind.session.SET_VOLUME"
        const val EXTRA_TASK = "task"
        const val EXTRA_POMODORO = "pomodoro"
        const val EXTRA_TRY_DND = "try_dnd"
        const val EXTRA_FOCUS_MINUTES = "focus_minutes"
        const val EXTRA_BREAK_MINUTES = "break_minutes"
        const val EXTRA_SOUND = "ambient_sound"
        const val EXTRA_VOLUME = "ambient_volume"
        const val EXTRA_PIN = "pin_requested"
        const val ACTION_OPEN_SESSION = "com.example.popmind.OPEN_SESSION"
        const val CHANNEL_ID = "focus_session"
        private const val REMINDER_CHANNEL_ID = "focus_reminder"
        const val NOTIFICATION_ID = 42
        private const val REMINDER_NOTIFICATION_ID = 43
        // Có thể bật cờ này để rút ngắn mọi giai đoạn còn 1 phút khi thử.
        const val DEBUG_SHORT_TIMER = false
        const val LOCK_SOFT_ENABLED = true
    }
}
