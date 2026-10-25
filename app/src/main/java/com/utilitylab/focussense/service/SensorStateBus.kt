package com.utilitylab.focussense.service

import com.utilitylab.focussense.domain.FocusRuntimeSnapshot
import com.utilitylab.focussense.domain.LiveSensorSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SensorStateBus {
    private val _live = MutableStateFlow(LiveSensorSnapshot())
    val live: StateFlow<LiveSensorSnapshot> = _live.asStateFlow()

    private val _runtime = MutableStateFlow(FocusRuntimeSnapshot())
    val runtime: StateFlow<FocusRuntimeSnapshot> = _runtime.asStateFlow()

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError: StateFlow<String?> = _sessionError.asStateFlow()

    private val _appForeground = MutableStateFlow(false)
    val appForeground: StateFlow<Boolean> = _appForeground.asStateFlow()

    fun publish(snapshot: LiveSensorSnapshot) {
        _live.value = snapshot
    }

    fun publishRuntime(snapshot: FocusRuntimeSnapshot) {
        _runtime.value = snapshot
    }

    fun setActiveSession(id: Long?) {
        _activeSessionId.value = id
    }

    fun setAppForeground(foreground: Boolean) {
        _appForeground.value = foreground
    }

    fun publishError(message: String) {
        _sessionError.value = message
    }

    fun clearError() {
        _sessionError.value = null
    }

    fun reset() {
        _live.value = LiveSensorSnapshot()
        _runtime.value = FocusRuntimeSnapshot()
    }
}
