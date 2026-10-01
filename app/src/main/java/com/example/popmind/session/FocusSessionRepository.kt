package com.example.popmind.session

import android.content.Context
import com.example.popmind.data.local.PopMindDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FocusSessionRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(FocusSessionState())
    val state = _state.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _openRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val persistenceLock = Mutex()
    val openRequests = _openRequests.asSharedFlow()
    @Volatile private var initialized = false
    private lateinit var database: PopMindDatabase

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            database = PopMindDatabase.getInstance(context.applicationContext)
            initialized = true
        }
        scope.launch {
            val entity = database.activeFocusDao().getActive()
            if (entity != null) _state.value = FocusSessionState.fromEntity(entity)
            _ready.value = true
        }
    }

    fun publish(state: FocusSessionState, persist: Boolean = false) {
        _state.value = state
        if (persist && initialized) scope.launch { persistenceLock.withLock { database.activeFocusDao().save(state.toEntity()) } }
    }

    fun clear(state: FocusSessionState = FocusSessionState()) {
        _state.value = state
        if (initialized) scope.launch { persistenceLock.withLock { database.activeFocusDao().clear() } }
    }

    fun requestOpenSession() { _openRequests.tryEmit(Unit) }
}
