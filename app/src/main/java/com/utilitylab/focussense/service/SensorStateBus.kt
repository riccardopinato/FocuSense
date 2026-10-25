package com.utilitylab.focussense.service

import com.utilitylab.focussense.domain.LiveSensorSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SensorStateBus {
    private val _live = MutableStateFlow(LiveSensorSnapshot())
    val live: StateFlow<LiveSensorSnapshot> = _live.asStateFlow()

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError: StateFlow<String?> = _sessionError.asStateFlow()

    fun publish(snapshot: LiveSensorSnapshot) {
        _live.value = snapshot
    }

    fun setActiveSession(id: Long?) {
        _activeSessionId.value = id
    }

    fun publishError(message: String) {
        _sessionError.value = message
    }

    fun clearError() {
        _sessionError.value = null
    }

    fun reset() {
        _live.value = LiveSensorSnapshot()
    }
}
