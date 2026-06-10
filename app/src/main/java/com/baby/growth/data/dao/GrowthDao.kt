package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.GrowthRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthDao {
    @Query("SELECT * FROM growth_records ORDER BY recordTime DESC")
    fun getAll(): Flow<List<GrowthRecord>>

    @Query("SELECT * FROM growth_records ORDER BY recordTime ASC")
    fun getAllAsc(): Flow<List<GrowthRecord>>

    @Query("SELECT * FROM growth_records ORDER BY recordTime ASC")
    suspend fun getAllAscOnce(): List<GrowthRecord>

    @Query("SELECT * FROM growth_records ORDER BY recordTime DESC")
    suspend fun getAllOnce(): List<GrowthRecord>

    @Query("SELECT * FROM growth_records ORDER BY recordTime DESC LIMIT 1")
    suspend fun getLatest(): GrowthRecord?

    @Query("SELECT * FROM growth_records WHERE recordTime BETWEEN :startTime AND :endTime ORDER BY recordTime DESC")
    suspend fun getByDateRangeOnce(startTime: Long, endTime: Long): List<GrowthRecord>

    @Query("SELECT * FROM growth_records WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): GrowthRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: GrowthRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<GrowthRecord>)

    @Update
    suspend fun update(record: GrowthRecord)

    @Delete
    suspend fun delete(record: GrowthRecord)

    @Query("DELETE FROM growth_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM growth_records")
    suspend fun deleteAll()
}