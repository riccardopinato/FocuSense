package com.utilitylab.focussense

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.utilitylab.focussense.data.FocusRepository
import com.utilitylab.focussense.data.UserPreferences
import com.utilitylab.focussense.data.local.AppDatabase
import com.utilitylab.focussense.service.SensorStateBus

class FocusSenseApp : Application(), DefaultLifecycleObserver {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: FocusRepository by lazy { FocusRepository(database) }
    val preferences: UserPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        SensorStateBus.setAppForeground(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        SensorStateBus.setAppForeground(false)
    }
}
