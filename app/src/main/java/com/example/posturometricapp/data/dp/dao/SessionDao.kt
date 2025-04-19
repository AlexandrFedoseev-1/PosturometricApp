package com.example.posturometricapp.data.dp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.posturometricapp.data.dp.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("UPDATE session SET endTime = :endTime WHERE id = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)

    @Query("SELECT * FROM session ORDER BY startTime DESC")
    fun getAllSession(): Flow<List<SessionEntity>>
}