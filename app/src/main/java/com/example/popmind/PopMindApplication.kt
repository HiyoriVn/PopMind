package com.example.popmind

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.popmind.session.FocusSessionRepository
import com.example.popmind.service.FocusSessionService

class PopMindApplication : Application(), Application.ActivityLifecycleCallbacks {
    private val handler = Handler(Looper.getMainLooper())
    private var startedActivities = 0
    private val notifyBackground = Runnable {
        if (startedActivities == 0 && FocusSessionRepository.state.value.isActive) {
            sendBroadcast(Intent(FocusSessionService.ACTION_APP_LEFT).setPackage(packageName))
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        handler.removeCallbacks(notifyBackground)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) handler.postDelayed(notifyBackground, 700L)
    }

    override fun onActivityCreated(activity: Activity, state: android.os.Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: android.os.Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
