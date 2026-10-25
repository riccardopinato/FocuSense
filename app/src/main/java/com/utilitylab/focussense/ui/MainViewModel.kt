package com.utilitylab.focussense.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utilitylab.focussense.FocusSenseApp
import com.utilitylab.focussense.data.AppPreferences
import com.utilitylab.focussense.data.SessionDetailData
import com.utilitylab.focussense.data.local.FocusSessionEntity
import com.utilitylab.focussense.domain.BestConditionsEngine
import com.utilitylab.focussense.domain.BestConditionsProfile
import com.utilitylab.focussense.domain.ContextAnalytics
import com.utilitylab.focussense.domain.ContextAnalyticsEngine
import com.utilitylab.focussense.domain.EnvironmentMatch
import com.utilitylab.focussense.domain.FocusStrictness
import com.utilitylab.focussense.domain.GamificationEngine
import com.utilitylab.focussense.domain.GamificationProgress
import com.utilitylab.focussense.domain.LiveSensorSnapshot
import com.utilitylab.focussense.domain.PersonalFocusProfile
import com.utilitylab.focussense.domain.SessionContext
import com.utilitylab.focussense.domain.SessionTagCodec
import com.utilitylab.focussense.domain.TemporalInsights
import com.utilitylab.focussense.domain.TemporalInsightsEngine
import com.utilitylab.focussense.domain.WeeklyGoalSettings
import com.utilitylab.focussense.monetization.MonetizationState
import com.utilitylab.focussense.service.FocusSessionService
import com.utilitylab.focussense.service.SensorStateBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FocusSenseApp

    val sessions: StateFlow<List<FocusSessionEntity>> = app.repository.sessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val preferences: StateFlow<AppPreferences> = app.preferences.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppPreferences()
    )

    val live: StateFlow<LiveSensorSnapshot> = SensorStateBus.live
    val runtime = SensorStateBus.runtime
    val monetization: StateFlow<MonetizationState> = app.monetization.state
    val activeSessionId: StateFlow<Long?> = SensorStateBus.activeSessionId
    val sessionError: StateFlow<String?> = SensorStateBus.sessionError

    private val _profile = MutableStateFlow(PersonalFocusProfile.generic())
    val personalProfile: StateFlow<PersonalFocusProfile> = _profile.asStateFlow()

    val temporalInsights: StateFlow<TemporalInsights> = sessions
        .map { TemporalInsightsEngine.build(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TemporalInsights()
        )

    val contextAnalytics: StateFlow<ContextAnalytics> = sessions
        .map { ContextAnalyticsEngine.build(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ContextAnalytics()
        )

    val gamificationProgress: StateFlow<GamificationProgress> = combine(
        sessions,
        personalProfile,
        preferences
    ) { sessionList, profile, prefs ->
        GamificationEngine.build(
            sessions = sessionList,
            profile = profile,
            goals = WeeklyGoalSettings(
                sessionGoal = prefs.weeklySessionGoal,
                minuteGoal = prefs.weeklyMinuteGoal
            )
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GamificationProgress()
    )

    val bestConditions: StateFlow<BestConditionsProfile> = sessions
        .map { BestConditionsEngine.build(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BestConditionsProfile()
        )

    val environmentMatch: StateFlow<EnvironmentMatch> = combine(
        bestConditions,
        live
    ) { profile, snapshot ->
        BestConditionsEngine.environmentMatch(
            profile = profile,
            live = snapshot,
            availability = snapshot.sensorsAvailable
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EnvironmentMatch()
    )

    private val _selectedSession = MutableStateFlow<SessionDetailData?>(null)
    val selectedSession: StateFlow<SessionDetailData?> = _selectedSession.asStateFlow()

    init {
        if (activeSessionId.value == null) {
            viewModelScope.launch {
                app.repository.deleteUnfinishedSessions()
                refreshPersonalProfile()
            }
        }
    }

    fun refreshPersonalProfile() {
        viewModelScope.launch {
            _profile.value = app.repository.buildPersonalProfile()
        }
    }

    fun openSessionDetail(sessionId: Long) {
        viewModelScope.launch {
            _selectedSession.value = app.repository.sessionDetail(sessionId)
        }
    }

    fun closeSessionDetail() {
        _selectedSession.value = null
    }

    fun startSession(
        context: Context,
        minutes: Int = preferences.value.defaultSessionMinutes,
        sessionContext: SessionContext = SessionContext.WORK,
        tags: List<String> = emptyList(),
        strictness: FocusStrictness = preferences.value.defaultStrictness,
        onStarted: (Long) -> Unit = {}
    ) {
        if (activeSessionId.value != null) return
        SensorStateBus.clearError()
        viewModelScope.launch {
            app.repository.cleanupRawSamples()
            val id = app.repository.createSession(
                plannedMinutes = minutes,
                context = sessionContext,
                tags = SessionTagCodec.normalize(tags),
                strictness = strictness
            )
            val p = preferences.value
            val intent = Intent(context, FocusSessionService::class.java).apply {
                action = FocusSessionService.ACTION_START
                putExtra(FocusSessionService.EXTRA_SESSION_ID, id)
                putExtra(FocusSessionService.EXTRA_POSTURE_MINUTES, p.postureReminderMinutes)
                putExtra(FocusSessionService.EXTRA_POSTURE_ENABLED, p.postureReminderEnabled)
                putExtra(FocusSessionService.EXTRA_PLANNED_MINUTES, minutes)
                putExtra(FocusSessionService.EXTRA_STRICTNESS, strictness.name)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onSuccess { onStarted(id) }
                .onFailure {
                    app.repository.deleteSession(id)
                    SensorStateBus.publishError(
                        "Impossibile avviare la sessione. Verifica i permessi e riprova."
                    )
                }
        }
    }

    fun stopSession(context: Context) {
        runCatching {
            context.startService(
                Intent(context, FocusSessionService::class.java)
                    .setAction(FocusSessionService.ACTION_STOP)
            )
        }.onFailure {
            SensorStateBus.publishError("Impossibile terminare la sessione correttamente.")
        }
    }

    fun saveRatings(
        sessionId: Long,
        focus: Int?,
        posture: Int?,
        note: String
    ) {
        viewModelScope.launch {
            app.repository.saveSelfRatings(sessionId, focus, posture, note)
            _profile.value = app.repository.buildPersonalProfile()
            _selectedSession.value = app.repository.sessionDetail(sessionId)
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            app.repository.deleteSession(id)
            if (_selectedSession.value?.session?.id == id) _selectedSession.value = null
            _profile.value = app.repository.buildPersonalProfile()
        }
    }

    fun setSessionMinutes(value: Int) {
        viewModelScope.launch { app.preferences.setDefaultSessionMinutes(value) }
    }

    fun setDefaultStrictness(value: FocusStrictness) {
        viewModelScope.launch { app.preferences.setDefaultStrictness(value) }
    }

    fun setFocusDisplayEnabled(enabled: Boolean) {
        viewModelScope.launch { app.preferences.setFocusDisplayEnabled(enabled) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { app.preferences.setOnboardingComplete(true) }
    }

    fun setPostureReminder(enabled: Boolean, minutes: Int) {
        viewModelScope.launch { app.preferences.setPostureReminder(enabled, minutes) }
    }

    fun setWeeklyGoals(sessionGoal: Int, minuteGoal: Int) {
        viewModelScope.launch {
            app.preferences.setWeeklyGoals(sessionGoal, minuteGoal)
        }
    }

    fun saveGoogleProfile(
        uniqueId: String,
        email: String,
        name: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            app.preferences.saveGoogleProfile(uniqueId, email, name, photoUri)
        }
    }

    fun clearGoogleProfile() {
        viewModelScope.launch { app.preferences.clearGoogleProfile() }
    }

    fun purchasePro(activity: Activity) {
        app.monetization.purchasePro(activity)
    }

    fun restorePurchases() {
        app.monetization.restorePurchases()
    }

    fun showPrivacyOptions(activity: Activity) {
        app.monetization.showPrivacyOptions(activity)
    }

    fun refreshConsent(activity: Activity) {
        app.monetization.refreshConsent(activity)
    }

    fun clearSessionError() {
        SensorStateBus.clearError()
    }
}
