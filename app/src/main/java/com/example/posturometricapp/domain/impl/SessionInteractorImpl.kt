package com.example.posturometricapp.domain.impl

import com.example.posturometricapp.domain.api.SessionInteractor
import com.example.posturometricapp.domain.api.SessionRepository
import com.example.posturometricapp.domain.model.SensorData
import com.example.posturometricapp.domain.model.Session
import kotlinx.coroutines.flow.Flow

class SessionInteractorImpl(
    private val repository: SessionRepository
) : SessionInteractor {
    override fun getAllSessions(): Flow<List<Session>> =
        repository.getAllSessions()

    override fun getSensorDataForSession(sessionId: Long): Flow<List<SensorData>> =
        repository.getSensorDataForSession(sessionId)
}