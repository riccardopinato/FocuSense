package com.utilitylab.focussense.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.utilitylab.focussense.domain.FocusStrictness
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusSenseDataStore by preferencesDataStore(name = "focus_sense_preferences")

data class AppPreferences(
    val defaultSessionMinutes: Int = 50,
    val defaultStrictness: FocusStrictness = FocusStrictness.STANDARD,
    val focusDisplayEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val postureReminderMinutes: Int = 30,
    val postureReminderEnabled: Boolean = false,
    val weeklySessionGoal: Int = 5,
    val weeklyMinuteGoal: Int = 150,
    val googleUniqueId: String = "",
    val googleEmail: String = "",
    val googleDisplayName: String = "",
    val googlePhotoUri: String = ""
) {
    val signedIn: Boolean get() = googleUniqueId.isNotBlank()
}

class UserPreferences(private val context: Context) {
    private object Keys {
        val defaultSession = intPreferencesKey("default_session_minutes")
        val defaultStrictness = stringPreferencesKey("default_strictness")
        val focusDisplayEnabled = booleanPreferencesKey("focus_display_enabled")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val postureReminder = intPreferencesKey("posture_reminder_minutes")
        val postureEnabled = booleanPreferencesKey("posture_reminder_enabled")
        val weeklySessionGoal = intPreferencesKey("weekly_session_goal")
        val weeklyMinuteGoal = intPreferencesKey("weekly_minute_goal")
        val googleUniqueId = stringPreferencesKey("google_unique_id")
        val googleEmail = stringPreferencesKey("google_email")
        val googleName = stringPreferencesKey("google_name")
        val googlePhoto = stringPreferencesKey("google_photo")
    }

    val state: Flow<AppPreferences> = context.focusSenseDataStore.data.map { it.toModel() }

    suspend fun setDefaultSessionMinutes(value: Int) {
        context.focusSenseDataStore.edit { it[Keys.defaultSession] = value.coerceIn(5, 180) }
    }

    suspend fun setDefaultStrictness(value: FocusStrictness) {
        context.focusSenseDataStore.edit { it[Keys.defaultStrictness] = value.name }
    }

    suspend fun setFocusDisplayEnabled(enabled: Boolean) {
        context.focusSenseDataStore.edit { it[Keys.focusDisplayEnabled] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean = true) {
        context.focusSenseDataStore.edit { it[Keys.onboardingComplete] = complete }
    }

    suspend fun setPostureReminder(enabled: Boolean, minutes: Int) {
        context.focusSenseDataStore.edit {
            it[Keys.postureEnabled] = enabled
            it[Keys.postureReminder] = minutes.coerceIn(10, 120)
        }
    }

    suspend fun setWeeklyGoals(sessionGoal: Int, minuteGoal: Int) {
        context.focusSenseDataStore.edit {
            it[Keys.weeklySessionGoal] = sessionGoal.coerceIn(1, 50)
            it[Keys.weeklyMinuteGoal] = minuteGoal.coerceIn(10, 5000)
        }
    }

    suspend fun saveGoogleProfile(uniqueId: String, email: String, name: String, photoUri: String?) {
        context.focusSenseDataStore.edit {
            it[Keys.googleUniqueId] = uniqueId
            it[Keys.googleEmail] = email
            it[Keys.googleName] = name
            it[Keys.googlePhoto] = photoUri.orEmpty()
        }
    }

    suspend fun clearGoogleProfile() {
        context.focusSenseDataStore.edit {
            it.remove(Keys.googleUniqueId)
            it.remove(Keys.googleEmail)
            it.remove(Keys.googleName)
            it.remove(Keys.googlePhoto)
        }
    }

    private fun Preferences.toModel() = AppPreferences(
        defaultSessionMinutes = this[Keys.defaultSession] ?: 50,
        defaultStrictness = FocusStrictness.fromKey(this[Keys.defaultStrictness]),
        focusDisplayEnabled = this[Keys.focusDisplayEnabled] ?: true,
        onboardingComplete = this[Keys.onboardingComplete] ?: false,
        postureReminderMinutes = this[Keys.postureReminder] ?: 30,
        postureReminderEnabled = this[Keys.postureEnabled] ?: false,
        weeklySessionGoal = this[Keys.weeklySessionGoal] ?: 5,
        weeklyMinuteGoal = this[Keys.weeklyMinuteGoal] ?: 150,
        googleUniqueId = this[Keys.googleUniqueId].orEmpty(),
        googleEmail = this[Keys.googleEmail].orEmpty(),
        googleDisplayName = this[Keys.googleName].orEmpty(),
        googlePhotoUri = this[Keys.googlePhoto].orEmpty()
    )
}
