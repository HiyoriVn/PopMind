package com.example.popmind.data

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

data class PersonalProfile(
    val name: String = "",
    val grade: Int = 10,
    val subjects: List<String> = emptyList(),
    val distractionWindow: String = "Tối",
    val favoriteApps: List<String> = emptyList(),
    val completed: Boolean = false
)

private val Application.personalizationStore by preferencesDataStore(name = "personalization")

class PersonalizationStore(private val app: Application) {
    private val nameKey = stringPreferencesKey("name")
    private val gradeKey = intPreferencesKey("grade")
    private val subjectsKey = stringPreferencesKey("subjects")
    private val windowKey = stringPreferencesKey("distraction_window")
    private val appsKey = stringPreferencesKey("favorite_apps")
    private val completeKey = booleanPreferencesKey("onboarding_complete")

    val profile = app.personalizationStore.data.map { prefs ->
        PersonalProfile(
            name = prefs[nameKey].orEmpty(), grade = prefs[gradeKey] ?: 10,
            subjects = prefs[subjectsKey].orEmpty().split('|').filter(String::isNotBlank),
            distractionWindow = prefs[windowKey] ?: "Tối",
            favoriteApps = prefs[appsKey].orEmpty().split('|').filter(String::isNotBlank),
            completed = prefs[completeKey] ?: false
        )
    }

    suspend fun save(profile: PersonalProfile) {
        app.personalizationStore.edit { prefs ->
            prefs[nameKey] = profile.name.trim()
            prefs[gradeKey] = profile.grade
            prefs[subjectsKey] = profile.subjects.distinct().joinToString("|")
            prefs[windowKey] = profile.distractionWindow
            prefs[appsKey] = profile.favoriteApps.distinct().joinToString("|")
            prefs[completeKey] = true
        }
    }

    fun installedApps(): List<InstalledAppChoice> {
        val pm = app.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == app.packageName) null else InstalledAppChoice(pkg, info.loadLabel(pm).toString())
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
}

data class InstalledAppChoice(val packageName: String, val label: String)
