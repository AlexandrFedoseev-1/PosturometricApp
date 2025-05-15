package com.example.posturometricapp.domain.impl


import com.example.posturometricapp.domain.api.PsychStateInteractor
import com.example.posturometricapp.domain.api.PsychStateRepository
import com.example.posturometricapp.domain.model.PsychState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow

class PsychStateInteractorImpl(
    private val repository: PsychStateRepository
) : PsychStateInteractor {
    override suspend fun startState(sessionId: Long, stateName: String): Long {
        return repository.startState(sessionId, stateName)
    }

    override suspend fun endState(stateId: Long) {
        repository.endState(stateId)
    }

    override suspend fun switchState(sessionId: Long, newStateName: String): Long {
        // Закрываем предыдущее активное состояние, если есть
        val states = repository.getStatesForSession(sessionId).first()
        val open = states.find { it.endTime == null }
        open?.let { repository.endState(it.id) }
        // Запускаем новое
        return repository.startState(sessionId, newStateName)
    }

    override fun getStatesForSession(sessionId: Long): Flow<List<PsychState>> {
        return repository.getStatesForSession(sessionId)
    }
}