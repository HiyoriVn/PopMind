package com.example.popmind.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppUsage(val packageName: String, val label: String, val foregroundMillis: Long)
data class UsageSummary(
    val hasAccess: Boolean = false,
    val loading: Boolean = false,
    val shortContentMillis: Long = 0,
    val topApps: List<AppUsage> = emptyList(),
    val shortContentByHour: Map<Int, Long> = emptyMap()
)

class UsageStatsRepository(context: Context) {
    private val app = context.applicationContext
    private val shortPackages = setOf(
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.google.android.youtube",
        "com.facebook.katana", "com.instagram.android"
    )

    fun hasAccess(): Boolean = try {
        val ops = app.getSystemService(AppOpsManager::class.java)
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }

    suspend fun readLastSevenDays(): UsageSummary {
        val end = System.currentTimeMillis()
        return readRange(end - 7L * 24 * 60 * 60 * 1000, end)
    }

    suspend fun readRange(start: Long, end: Long): UsageSummary = withContext(Dispatchers.IO) {
        if (!hasAccess()) return@withContext UsageSummary()
        try {
            val stats = app.getSystemService(UsageStatsManager::class.java)
                .queryAndAggregateUsageStats(start, end).orEmpty()
            val entries = stats.mapNotNull { (pkg, stat) ->
                val millis = stat.totalTimeInForeground
                if (millis <= 0L) return@mapNotNull null
                val label = try {
                    app.packageManager.getApplicationLabel(app.packageManager.getApplicationInfo(pkg, 0)).toString()
                } catch (_: PackageManager.NameNotFoundException) { pkg }
                AppUsage(pkg, label, millis)
            }
            val shortByHour = readShortContentByHour(start, end)
            UsageSummary(true, false, entries.filter { it.packageName in shortPackages }.sumOf { it.foregroundMillis },
                entries.sortedByDescending { it.foregroundMillis }.take(5), shortByHour)
        } catch (_: SecurityException) { UsageSummary() }
    }

    // Ước lượng phân bổ theo giờ từ các mốc app vào/ra foreground trong tuần.
    private fun readShortContentByHour(start: Long, end: Long): Map<Int, Long> {
        val manager = app.getSystemService(UsageStatsManager::class.java)
        val events = manager.queryEvents(start, end)
        val event = android.app.usage.UsageEvents.Event()
        val totals = LongArray(24)
        var activePackage: String? = null
        var activeSince = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val foreground = event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= 29 && event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED)
            val background = event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND ||
                (Build.VERSION.SDK_INT >= 29 && event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED)
            if (foreground) {
                if (activePackage?.let(shortPackages::contains) == true && event.timeStamp > activeSince) addInterval(totals, activeSince, event.timeStamp)
                activePackage = event.packageName
                activeSince = event.timeStamp
            } else if (background && event.packageName == activePackage) {
                if (activePackage?.let(shortPackages::contains) == true && event.timeStamp > activeSince) addInterval(totals, activeSince, event.timeStamp)
                activePackage = null
                activeSince = 0L
            }
        }
        if (activePackage?.let(shortPackages::contains) == true && end > activeSince) addInterval(totals, activeSince, end)
        val result: MutableMap<Int, Long> = linkedMapOf()
        for (index in totals.indices) {
            val hour: Int = index
            val millis: Long = totals[hour]
            if (millis > 0L) result[hour] = millis
        }
        return result
    }

    private fun addInterval(totals: LongArray, start: Long, end: Long) {
        var cursor = start
        val zone = java.time.ZoneId.systemDefault()
        while (cursor < end) {
            val zoned = java.time.Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHour = zoned.withMinute(0).withSecond(0).withNano(0).plusHours(1).toInstant().toEpochMilli()
            val until = minOf(end, nextHour)
            totals[zoned.hour] += until - cursor
            cursor = until
        }
    }
}
