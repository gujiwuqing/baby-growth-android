package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.BabyInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyInfoDao {
    @Query("SELECT * FROM baby_info LIMIT 1")
    fun getBabyInfo(): Flow<BabyInfo?>

    @Query("SELECT * FROM baby_info LIMIT 1")
    suspend fun getBabyInfoOnce(): BabyInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(babyInfo: BabyInfo): Long

    @Update
    suspend fun update(babyInfo: BabyInfo)

    @Query("DELETE FROM baby_info")
    suspend fun deleteAll()
}