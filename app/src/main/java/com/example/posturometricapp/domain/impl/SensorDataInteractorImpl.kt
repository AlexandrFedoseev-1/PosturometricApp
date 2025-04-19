package com.example.posturometricapp.domain.impl

import com.example.posturometricapp.domain.api.SensorDataInteractor
import com.example.posturometricapp.domain.api.SensorDataRepository
import com.example.posturometricapp.domain.model.SensorData
import kotlinx.coroutines.flow.Flow

class SensorDataInteractorImpl(private val repository: SensorDataRepository): SensorDataInteractor {
    override suspend fun startSession(): Long {
        return repository.startSession()
    }

    override suspend fun stopSession(sessionId: Long) {
        repository.stopSession(sessionId)
    }

    override fun getLiveSensorData(): Flow<SensorData> {
        return repository.getLiveSensorData()
    }

    override suspend fun saveSensorData(sessionId: Long, sensorData: SensorData) {
        repository.saveSensorData(sessionId,sensorData)
    }

    override suspend fun sendEnableReadingCommand(): Boolean {
        return repository.sendEnableReadingCommand()
    }

    override suspend fun sendStopReadingCommand(): Boolean {
        return repository.sendStopReadingCommand()
    }

    override suspend fun sendCalibrationCommand(): Boolean {
        return repository.sendCalibrationCommand()
    }

    override suspend fun calibrateSensors(): Boolean {
        return repository.calibrateSensors()
    }
}