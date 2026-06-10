package com.baby.growth.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.baby.growth.data.dao.*
import com.baby.growth.data.entity.*

@Database(
    entities = [
        BabyInfo::class,
        FeedRecord::class,
        DiaperRecord::class,
        SleepRecord::class,
        FoodRecord::class,
        SupplementRecord::class,
        GrowthRecord::class,
        VaccineRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun babyInfoDao(): BabyInfoDao
    abstract fun feedDao(): FeedDao
    abstract fun diaperDao(): DiaperDao
    abstract fun sleepDao(): SleepDao
    abstract fun foodDao(): FoodDao
    abstract fun supplementDao(): SupplementDao
    abstract fun growthDao(): GrowthDao
    abstract fun vaccineDao(): VaccineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "baby_growth_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}