package com.example.posturometricapp.data.dp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.posturometricapp.data.dp.entity.SensorDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorData(sensorDataEntity: SensorDataEntity)

    @Query("SELECT * FROM sensor_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSensorDataForSession(sessionId: Long): Flow<List<SensorDataEntity>>
}