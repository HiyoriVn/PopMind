package com.example.popmind.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppUsage(val packageName: String, val label: String, val foregroundMillis: Long)
data class UsageSummary(
    val hasAccess: Boolean = false,
    val loading: Boolean = false,
    val shortContentMillis: Long = 0,
    val topApps: List<AppUsage> = emptyList()
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

    suspend fun readLastSevenDays(): UsageSummary = withContext(Dispatchers.IO) {
        if (!hasAccess()) return@withContext UsageSummary()
        try {
            val end = System.currentTimeMillis()
            val start = end - 7L * 24 * 60 * 60 * 1000
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
            UsageSummary(true, false, entries.filter { it.packageName in shortPackages }.sumOf { it.foregroundMillis },
                entries.sortedByDescending { it.foregroundMillis }.take(5))
        } catch (_: SecurityException) { UsageSummary() }
    }
}
