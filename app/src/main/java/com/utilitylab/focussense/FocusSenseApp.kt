package com.utilitylab.focussense

import android.app.Application
import com.utilitylab.focussense.data.FocusRepository
import com.utilitylab.focussense.data.UserPreferences
import com.utilitylab.focussense.data.local.AppDatabase

class FocusSenseApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: FocusRepository by lazy { FocusRepository(database) }
    val preferences: UserPreferences by lazy { UserPreferences(this) }
}
