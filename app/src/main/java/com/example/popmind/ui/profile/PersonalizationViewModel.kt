package com.example.popmind.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.popmind.data.InstalledAppChoice
import com.example.popmind.data.PersonalProfile
import com.example.popmind.data.PersonalizationStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonalizationUiState(val profile: PersonalProfile = PersonalProfile(), val loaded: Boolean = false, val installedApps: List<InstalledAppChoice> = emptyList())

class PersonalizationViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PersonalizationStore(application)
    val state: StateFlow<PersonalizationUiState> = store.profile
        .map { PersonalizationUiState(it, true, store.installedApps()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PersonalizationUiState())

    fun save(profile: PersonalProfile, onSaved: () -> Unit = {}) {
        viewModelScope.launch { store.save(profile); onSaved() }
    }
}
