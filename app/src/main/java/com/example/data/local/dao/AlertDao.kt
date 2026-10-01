package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isActive = 1")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isActive = 1")
    suspend fun getActiveAlertsDirect(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("UPDATE alerts SET isTriggered = 1, isActive = 0, triggeredAt = :timestamp WHERE id = :id")
    suspend fun markAlertTriggered(id: Long, timestamp: Long)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()
}
