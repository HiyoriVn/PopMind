package com.example.popmind.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.popmind.data.InstalledAppChoice
import com.example.popmind.data.PersonalProfile
import com.example.popmind.data.PersonalizationStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonalizationUiState(
    val profile: PersonalProfile = PersonalProfile(),
    val loaded: Boolean = false,
    val installedApps: List<InstalledAppChoice> = emptyList(),
    val appsLoading: Boolean = true,
    val appsLoadFailed: Boolean = false
)

class PersonalizationViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PersonalizationStore(application)
    private val _state = MutableStateFlow(PersonalizationUiState())
    val state: StateFlow<PersonalizationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.profile.collect { profile -> _state.update { it.copy(profile = profile, loaded = true) } }
        }
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(appsLoading = true, appsLoadFailed = false) }
            val result = runCatching { store.installedApps() }
            val apps = result.getOrDefault(emptyList())
            if (result.isSuccess) {
                runCatching { store.reconcileLinkedApps(apps.mapTo(mutableSetOf()) { it.packageName }) }
            }
            _state.update {
                it.copy(installedApps = apps, appsLoading = false, appsLoadFailed = result.isFailure)
            }
        }
    }

    fun save(profile: PersonalProfile, onSaved: () -> Unit = {}) {
        viewModelScope.launch { store.save(profile); onSaved() }
    }
}
