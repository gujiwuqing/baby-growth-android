package com.baby.growth

import android.app.Application
import com.baby.growth.data.database.AppDatabase
import com.baby.growth.utils.BreastfeedingTimer
import com.baby.growth.utils.SleepTimer

class BabyGrowthApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        BreastfeedingTimer.initWithContext(this)
        SleepTimer.initWithContext(this)
    }
}