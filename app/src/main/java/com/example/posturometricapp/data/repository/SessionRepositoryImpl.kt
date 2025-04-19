package com.example.posturometricapp.data.repository

import com.example.posturometricapp.data.dp.AppDatabase
import com.example.posturometricapp.data.dp.dao.SensorDataDao
import com.example.posturometricapp.data.dp.dao.SessionDao
import com.example.posturometricapp.data.dp.entity.SensorDataEntity
import com.example.posturometricapp.data.dp.entity.SessionEntity
import com.example.posturometricapp.domain.api.SessionRepository
import com.example.posturometricapp.domain.model.SensorData
import com.example.posturometricapp.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl(
    private val appDatabase: AppDatabase
) : SessionRepository {
    override fun getAllSessions(): Flow<List<Session>> =
        appDatabase.sessionDao().getAllSession().map { list -> list.map { it.toDomain() } }

//    override fun getSessionById(sessionId: Long): Flow<SessionEntity?> =
//        sessionDao.getSessionById(sessionId)

    override fun getSensorDataForSession(sessionId: Long): Flow<List<SensorData>> =
        appDatabase.sensorDao().getSensorDataForSession(sessionId).map { list -> list.map { it.toDomain() } }
   private  fun SessionEntity.toDomain() = Session(
        id = id,
        startTime = startTime,
        endTime = endTime
    )

    private fun SensorDataEntity.toDomain() = SensorData(
        timestamp = timestamp,
        sensorValues = sensorValues,
        temperature = temperature
    )
}