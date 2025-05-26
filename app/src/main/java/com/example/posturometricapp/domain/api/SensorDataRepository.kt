package com.example.posturometricapp.domain.api


import com.example.posturometricapp.domain.model.SensorData
import kotlinx.coroutines.flow.Flow

interface SensorDataRepository {
    suspend fun startSession(): Long
    suspend fun stopSession(sessionId: Long)
    fun getLiveSensorData(): Flow<SensorData>
    suspend fun saveSensorData(sessionId: Long, sensorData: SensorData)
    suspend fun sendEnableReadingCommand(): Boolean
    suspend fun sendStopReadingCommand(): Boolean
    suspend fun calibrateSensors(): Boolean
    suspend fun sendCalibrationCommand(): Boolean
}