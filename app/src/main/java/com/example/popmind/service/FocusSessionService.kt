package com.example.popmind.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.app.NotificationManager as AndroidNotificationManager
import androidx.core.app.NotificationCompat
import com.example.popmind.data.local.PopMindDatabase
import com.example.popmind.data.local.SessionEntity
import com.example.popmind.MainActivity
import com.example.popmind.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusSessionService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: android.content.SharedPreferences
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotificationMinute = -1L
    private val databaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionWriteJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_END -> finishSession(completed = true)
            ACTION_ABANDON -> finishSession(completed = false)
            ACTION_INTERRUPT -> {
                val count = prefs.getInt(KEY_INTERRUPTS, 0) + 1
                prefs.edit().putInt(KEY_INTERRUPTS, count).apply()
                sendUpdate()
            }
            ACTION_TOGGLE_MUSIC -> toggleMusic()
            ACTION_REFRESH -> {
                if (prefs.getBoolean(KEY_ACTIVE, false)) {
                    acquireWakeLock()
                    startForegroundCompat(buildNotification())
                    startTicker()
                    sendUpdate()
                }
            }
            ACTION_START -> startSession(intent)
            else -> if (prefs.getBoolean(KEY_ACTIVE, false)) {
                acquireWakeLock()
                if (prefs.getBoolean(KEY_MUSIC, false)) startMusic()
                startForegroundCompat(buildNotification())
                startTicker()
            } else stopSelf()
        }
        return START_STICKY
    }

    private fun startSession(intent: Intent) {
        val now = System.currentTimeMillis()
        lastNotificationMinute = -1L
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_POMODORO, intent.getBooleanExtra(EXTRA_POMODORO, true))
            .putBoolean(KEY_MUSIC, intent.getBooleanExtra(EXTRA_MUSIC, false))
            .putString(KEY_TASK, intent.getStringExtra(EXTRA_TASK).orEmpty().ifBlank { "Phiên tập trung" })
            .putString(KEY_PHASE, PHASE_FOCUS)
            .putLong(KEY_SEGMENT_START, now)
            .putLong(KEY_STARTED_AT, now)
            .putInt(KEY_INTERRUPTS, 0)
            .putLong(KEY_SESSION_ID, -1L)
            .apply()

        val session = SessionEntity(
            startTime = now,
            durationSeconds = 0L,
            task = intent.getStringExtra(EXTRA_TASK).orEmpty().ifBlank { "Phiên tập trung" },
            interruptions = 0,
            usedPomodoro = intent.getBooleanExtra(EXTRA_POMODORO, true),
            completed = false
        )
        // Tạo bản ghi ngay để phiên vẫn được giữ lại nếu app bị đóng đột ngột.
        sessionWriteJob = databaseScope.launch {
            val id = PopMindDatabase.getInstance(applicationContext).sessionDao().insertSession(session)
            prefs.edit().putLong(KEY_SESSION_ID, id).apply()
        }

        if (intent.getBooleanExtra(EXTRA_TRY_DND, true)) enableDnd()
        acquireWakeLock()
        startForegroundCompat(buildNotification())
        if (prefs.getBoolean(KEY_MUSIC, false)) startMusic()
        sendUpdate()
        startTicker()
    }

    private fun enableDnd() {
        val manager = getSystemService(AndroidNotificationManager::class.java)
        if (manager.isNotificationPolicyAccessGranted) {
            val previous = manager.currentInterruptionFilter
            prefs.edit().putInt(KEY_OLD_FILTER, previous).putBoolean(KEY_CHANGED_DND, true).apply()
            manager.setInterruptionFilter(AndroidNotificationManager.INTERRUPTION_FILTER_NONE)
        } else {
            prefs.edit().putBoolean(KEY_CHANGED_DND, false).apply()
        }
    }

    private fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!prefs.getBoolean(KEY_ACTIVE, false)) return
            advancePomodoroIfNeeded()
            sendUpdate()
            val elapsedMinute = (System.currentTimeMillis() - prefs.getLong(KEY_STARTED_AT, 0L)) / 60_000L
            if (elapsedMinute != lastNotificationMinute) {
                lastNotificationMinute = elapsedMinute
                updateNotification()
            }
            handler.postDelayed(this, 1_000L)
        }
    }

    private fun advancePomodoroIfNeeded() {
        if (!prefs.getBoolean(KEY_POMODORO, true)) return
        val duration = if (prefs.getString(KEY_PHASE, PHASE_FOCUS) == PHASE_FOCUS) FOCUS_MS else BREAK_MS
        val segmentStart = prefs.getLong(KEY_SEGMENT_START, System.currentTimeMillis())
        if (System.currentTimeMillis() - segmentStart >= duration) {
            val phase = if (prefs.getString(KEY_PHASE, PHASE_FOCUS) == PHASE_FOCUS) PHASE_BREAK else PHASE_FOCUS
            prefs.edit().putString(KEY_PHASE, phase).putLong(KEY_SEGMENT_START, segmentStart + duration).apply()
            updateNotification()
        }
    }

    private fun sendUpdate() {
        val now = System.currentTimeMillis()
        val phase = prefs.getString(KEY_PHASE, PHASE_FOCUS) ?: PHASE_FOCUS
        val segmentStart = prefs.getLong(KEY_SEGMENT_START, now)
        val pomodoro = prefs.getBoolean(KEY_POMODORO, true)
        val totalElapsed = ((now - prefs.getLong(KEY_STARTED_AT, now)) / 1000L).coerceAtLeast(0L)
        val remaining = if (pomodoro) {
            val duration = if (phase == PHASE_FOCUS) FOCUS_MS else BREAK_MS
            ((duration - (now - segmentStart)).coerceAtLeast(0L) / 1000L)
        } else totalElapsed
        val update = Intent(ACTION_UPDATE).setPackage(packageName)
            .putExtra(EXTRA_TASK, prefs.getString(KEY_TASK, "").orEmpty())
            .putExtra(EXTRA_PHASE, phase)
            .putExtra(EXTRA_REMAINING, remaining)
            .putExtra(EXTRA_ELAPSED, totalElapsed)
            .putExtra(EXTRA_INTERRUPTS, prefs.getInt(KEY_INTERRUPTS, 0))
            .putExtra(EXTRA_MUSIC, prefs.getBoolean(KEY_MUSIC, false))
        sendBroadcast(update)
    }

    private fun toggleMusic() {
        val enabled = !prefs.getBoolean(KEY_MUSIC, false)
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply()
        if (enabled) startMusic() else stopMusic()
        sendUpdate()
    }

    private fun startMusic() {
        if (player != null) return
        player = MediaPlayer.create(this, R.raw.popmind_ambient).apply {
            isLooping = true
            setVolume(0.35f, 0.35f)
            start()
        }
    }

    private fun stopMusic() {
        player?.release()
        player = null
    }

    private fun finishSession(completed: Boolean) {
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return
        val now = System.currentTimeMillis()
        val elapsed = ((now - prefs.getLong(KEY_STARTED_AT, now)) / 1000L).coerceAtLeast(0L)
        val interruptions = prefs.getInt(KEY_INTERRUPTS, 0)
        persistSessionResult(elapsed, interruptions, completed)
        if (completed) {
            sendBroadcast(Intent(ACTION_ENDED).setPackage(packageName)
                .putExtra(EXTRA_ELAPSED, elapsed)
                .putExtra(EXTRA_INTERRUPTS, interruptions))
        } else {
            sendBroadcast(Intent(ACTION_CANCELLED).setPackage(packageName))
        }

        restoreDnd()
        prefs.edit().putBoolean(KEY_ACTIVE, false).putBoolean(KEY_CHANGED_DND, false).apply()
        stopMusic()
        handler.removeCallbacks(ticker)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun persistSessionResult(durationSeconds: Long, interruptions: Int, completed: Boolean) {
        val previousWrite = sessionWriteJob
        sessionWriteJob = databaseScope.launch {
            previousWrite?.join()
            val id = prefs.getLong(KEY_SESSION_ID, -1L)
            if (id > 0L) {
                PopMindDatabase.getInstance(applicationContext).sessionDao()
                    .updateSessionResult(id, durationSeconds, interruptions, completed)
            }
        }
    }

    private fun restoreDnd() {
        if (prefs.getBoolean(KEY_CHANGED_DND, false)) {
            val manager = getSystemService(AndroidNotificationManager::class.java)
            if (manager.isNotificationPolicyAccessGranted) {
                manager.setInterruptionFilter(prefs.getInt(KEY_OLD_FILTER, AndroidNotificationManager.INTERRUPTION_FILTER_ALL))
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        finishSession(completed = false)
        super.onTaskRemoved(rootIntent)
    }

    private fun acquireWakeLock() {
        val power = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:focus-session").apply {
            setReferenceCounted(false)
            acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun buildNotification(): Notification {
        val phase = prefs.getString(KEY_PHASE, PHASE_FOCUS) ?: PHASE_FOCUS
        val now = System.currentTimeMillis()
        val pomo = prefs.getBoolean(KEY_POMODORO, true)
        val started = prefs.getLong(KEY_SEGMENT_START, now)
        val duration = if (phase == PHASE_FOCUS) FOCUS_MS else BREAK_MS
        val clockText = if (pomo) formatClock(((duration - (now - started)).coerceAtLeast(0L) / 1000L))
            else formatClock((now - prefs.getLong(KEY_STARTED_AT, now)) / 1000L)
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), pendingFlags())
        val end = PendingIntent.getService(this, 1, Intent(this, FocusSessionService::class.java).setAction(ACTION_END), pendingFlags())
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (phase == PHASE_FOCUS) "Đang tập trung" else "Đang nghỉ")
            .setContentText("$clockText · ${prefs.getString(KEY_TASK, "Phiên tập trung")}")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Kết thúc", end)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Phiên tập trung", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Thông báo đồng hồ phiên tập trung đang chạy"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun pendingFlags() = PendingIntent.FLAG_UPDATE_CURRENT or
        (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)

    private fun formatClock(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (prefs.getBoolean(KEY_ACTIVE, false)) {
            val elapsed = ((System.currentTimeMillis() - prefs.getLong(KEY_STARTED_AT, System.currentTimeMillis())) / 1000L).coerceAtLeast(0L)
            persistSessionResult(elapsed, prefs.getInt(KEY_INTERRUPTS, 0), completed = false)
            restoreDnd()
            prefs.edit().putBoolean(KEY_ACTIVE, false).putBoolean(KEY_CHANGED_DND, false).apply()
        }
        handler.removeCallbacks(ticker)
        stopMusic()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.popmind.session.START"
        const val ACTION_END = "com.example.popmind.session.END"
        const val ACTION_ABANDON = "com.example.popmind.session.ABANDON"
        const val ACTION_INTERRUPT = "com.example.popmind.session.INTERRUPT"
        const val ACTION_TOGGLE_MUSIC = "com.example.popmind.session.TOGGLE_MUSIC"
        const val ACTION_REFRESH = "com.example.popmind.session.REFRESH"
        const val ACTION_UPDATE = "com.example.popmind.session.UPDATE"
        const val ACTION_ENDED = "com.example.popmind.session.ENDED"
        const val ACTION_CANCELLED = "com.example.popmind.session.CANCELLED"
        const val EXTRA_TASK = "task"
        const val EXTRA_POMODORO = "pomodoro"
        const val EXTRA_MUSIC = "music"
        const val EXTRA_TRY_DND = "try_dnd"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_REMAINING = "remaining"
        const val EXTRA_ELAPSED = "elapsed"
        const val EXTRA_INTERRUPTS = "interruptions"
        const val PREFS = "focus_session"
        const val CHANNEL_ID = "focus_session"
        const val NOTIFICATION_ID = 42
        const val PHASE_FOCUS = "focus"
        const val PHASE_BREAK = "break"
        private const val KEY_ACTIVE = "active"
        private const val KEY_POMODORO = "pomodoro"
        private const val KEY_MUSIC = "music"
        private const val KEY_TASK = "task"
        private const val KEY_PHASE = "phase"
        private const val KEY_SEGMENT_START = "segment_start"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_INTERRUPTS = "interruptions"
        private const val KEY_OLD_FILTER = "old_filter"
        private const val KEY_CHANGED_DND = "changed_dnd"
        private const val KEY_SESSION_ID = "session_id"
        private const val FOCUS_MS = 25 * 60 * 1000L
        private const val BREAK_MS = 5 * 60 * 1000L
    }
}
