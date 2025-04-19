package com.example.posturometricapp.domain.api


import com.example.posturometricapp.domain.model.SensorData
import com.example.posturometricapp.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionInteractor {
    fun getAllSessions():  Flow<List<Session>>
    fun getSensorDataForSession(sessionId: Long): Flow<List<SensorData>>
}