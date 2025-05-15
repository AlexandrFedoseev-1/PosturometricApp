package com.example.posturometricapp.domain.api

import com.example.posturometricapp.domain.model.PsychState
import kotlinx.coroutines.flow.Flow

interface PsychStateInteractor {
    suspend fun startState(sessionId: Long, stateName: String): Long
    suspend fun endState(stateId: Long)
    suspend fun switchState(sessionId: Long, newStateName: String): Long
    fun getStatesForSession(sessionId: Long): Flow<List<PsychState>>
}