package com.baby.growth.data.dao

import androidx.room.*
import com.baby.growth.data.entity.VaccineRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccines ORDER BY scheduledDate ASC")
    fun getAll(): Flow<List<VaccineRecord>>

    @Query("SELECT * FROM vaccines ORDER BY scheduledDate ASC")
    suspend fun getAllOnce(): List<VaccineRecord>

    @Query("SELECT * FROM vaccines WHERE vaccineName = :name")
    fun getByName(name: String): Flow<List<VaccineRecord>>

    @Query("SELECT * FROM vaccines WHERE vaccineType = :type ORDER BY scheduledDate ASC")
    suspend fun getByType(type: String): List<VaccineRecord>

    @Query("SELECT COUNT(*) FROM vaccines WHERE status = 'done'")
    suspend fun getDoneCount(): Int

    @Query("SELECT COUNT(*) FROM vaccines")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM vaccines WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getByUniqueId(uniqueId: String): VaccineRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: VaccineRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<VaccineRecord>)

    @Update
    suspend fun update(record: VaccineRecord)

    @Delete
    suspend fun delete(record: VaccineRecord)

    @Query("DELETE FROM vaccines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vaccines")
    suspend fun deleteAll()
}