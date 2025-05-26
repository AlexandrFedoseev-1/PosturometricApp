package com.example.posturometricapp.domain.api

import com.example.posturometricapp.domain.model.PsychState
import kotlinx.coroutines.flow.Flow

interface PsychStateRepository {
    suspend fun startState(sessionId: Long, stateName: String): Long
    suspend fun endState(stateId: Long)
    fun getStatesForSession(sessionId: Long): Flow<List<PsychState>>
}