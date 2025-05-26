package com.example.posturometricapp.domain.api


import com.example.posturometricapp.domain.model.SensorData
import com.example.posturometricapp.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    suspend fun deleteSession(session: Session)
    fun getSensorDataForSession(sessionId: Long): Flow<List<SensorData>>
}
