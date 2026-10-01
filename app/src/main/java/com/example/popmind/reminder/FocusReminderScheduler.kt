package com.example.popmind.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.popmind.MainActivity
import com.example.popmind.data.PersonalProfile
import java.time.ZonedDateTime

object FocusReminderScheduler {
    private const val CHANNEL = "focus_reminders"
    private const val REQUEST_CODE = 7408
    const val EXTRA_NAME = "name"
    const val EXTRA_WINDOW = "window"
    const val EXTRA_SUBJECT = "subject"
    private const val EXTRA_TARGET_HOUR = "target_hour"
    private const val EXTRA_TARGET_MINUTE = "target_minute"

    fun schedule(context: Context, profile: PersonalProfile) {
        val targetHour = when (profile.distractionWindow) { "Sáng" -> 7; "Trưa" -> 10; "Khuya" -> 21; else -> 16 }
        val now = ZonedDateTime.now()
        var trigger = now.withHour(targetHour).withMinute(30).withSecond(0).withNano(0)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        val intent = Intent(context, FocusReminderReceiver::class.java)
            .putExtra(EXTRA_NAME, profile.name)
            .putExtra(EXTRA_WINDOW, profile.distractionWindow)
            .putExtra(EXTRA_SUBJECT, profile.subjects.firstOrNull().orEmpty())
            .putExtra(EXTRA_TARGET_HOUR, targetHour).putExtra(EXTRA_TARGET_MINUTE, 30)
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toInstant().toEpochMilli(), pending)
    }

    internal fun notifyAndReschedule(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Nhắc giờ tập trung", NotificationManager.IMPORTANCE_DEFAULT))
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { "bạn" }
        val window = intent.getStringExtra(EXTRA_WINDOW).orEmpty().ifBlank { "đã chọn" }
        val subject = intent.getStringExtra(EXTRA_SUBJECT).orEmpty()
        val detail = if (subject.isBlank()) "Mình bắt đầu một phiên ngắn nhé?" else "Mình dành một khoảng cho $subject nhé?"
        val openApp = PendingIntent.getActivity(context, REQUEST_CODE, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(REQUEST_CODE, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${name}, đến giờ giữ nhịp rồi!")
            .setContentText("Trước khung giờ $window bạn hay xao nhãng: $detail")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Trước khung giờ $window bạn hay xao nhãng. $detail"))
            .setContentIntent(openApp).setAutoCancel(true).build())
        val next = intent.getIntExtra(EXTRA_TARGET_HOUR, when (window) { "Sáng" -> 7; "Trưa" -> 10; "Khuya" -> 21; else -> 16 })
        val minute = intent.getIntExtra(EXTRA_TARGET_MINUTE, 30)
        var trigger = ZonedDateTime.now().withHour(next).withMinute(minute).withSecond(0).withNano(0)
        if (!trigger.isAfter(ZonedDateTime.now())) trigger = trigger.plusDays(1)
        val alarmIntent = Intent(context, FocusReminderReceiver::class.java)
            .putExtra(EXTRA_NAME, name).putExtra(EXTRA_WINDOW, window).putExtra(EXTRA_SUBJECT, subject)
            .putExtra(EXTRA_TARGET_HOUR, next).putExtra(EXTRA_TARGET_MINUTE, minute)
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toInstant().toEpochMilli(), pending)
    }
}

class FocusReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = FocusReminderScheduler.notifyAndReschedule(context, intent)
}
