package com.example.popmind.session

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusSettings(val focusMinutes: Int = 25, val breakMinutes: Int = 5, val sound: String = FocusSessionState.SOUND_OFF, val volume: Float = .35f)

private val Application.focusSettingsStore by preferencesDataStore(name = "focus_settings")

class FocusSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val focusKey = intPreferencesKey("focus_minutes")
    private val breakKey = intPreferencesKey("break_minutes")
    private val soundKey = stringPreferencesKey("ambient_sound")
    private val volumeKey = floatPreferencesKey("ambient_volume")

    val settings: StateFlow<FocusSettings> = application.focusSettingsStore.data.map { values ->
        FocusSettings(values[focusKey] ?: 25, values[breakKey] ?: 5, values[soundKey] ?: FocusSessionState.SOUND_OFF, values[volumeKey] ?: .35f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusSettings())

    fun saveFocus(minutes: Int) = update { it[focusKey] = minutes.coerceIn(5, 120) }
    fun saveBreak(minutes: Int) = update { it[breakKey] = minutes.coerceIn(5, 10) }
    fun saveSound(sound: String) = update { it[soundKey] = sound }
    fun saveVolume(volume: Float) = update { it[volumeKey] = volume.coerceIn(0f, 1f) }

    private fun update(change: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { getApplication<Application>().focusSettingsStore.edit { change(it) } }
    }
}
