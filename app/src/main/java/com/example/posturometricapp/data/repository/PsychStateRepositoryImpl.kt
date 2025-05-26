package com.example.posturometricapp.data.repository

import com.example.posturometricapp.data.dp.AppDatabase
import com.example.posturometricapp.data.dp.entity.PsychStateEntity
import com.example.posturometricapp.domain.api.PsychStateRepository
import com.example.posturometricapp.domain.model.PsychState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PsychStateRepositoryImpl(
    private val appDatabase: AppDatabase
) : PsychStateRepository {
    private val dao = appDatabase.psychStateDao()

    override suspend fun startState(sessionId: Long, stateName: String): Long {
        val entity = PsychStateEntity(
            sessionId = sessionId,
            stateName = stateName,
            startTime = System.currentTimeMillis()
        )
        return dao.insertState(entity)
    }

    override suspend fun endState(stateId: Long) {
        dao.updateStateEndTime(stateId, System.currentTimeMillis())
    }

    override fun getStatesForSession(sessionId: Long): Flow<List<PsychState>> =
        dao.getStatesForSession(sessionId).map { list ->
            list.map { ent ->
                PsychState(
                    id = ent.id,
                    sessionId = ent.sessionId,
                    stateName = ent.stateName,
                    startTime = ent.startTime,
                    endTime = if (ent.endTime == 0L) null else ent.endTime
                )
            }
        }
}
