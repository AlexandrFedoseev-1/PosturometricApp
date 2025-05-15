package com.example.posturometricapp.data.dp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.posturometricapp.data.dp.entity.PsychStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PsychStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: PsychStateEntity): Long

    @Query("UPDATE psych_state SET endTime = :endTime WHERE id = :stateId")
    suspend fun updateStateEndTime(stateId: Long, endTime: Long)

    @Query("SELECT * FROM psych_state WHERE sessionId = :sessionId ORDER BY startTime ASC")
    fun getStatesForSession(sessionId: Long): Flow<List<PsychStateEntity>>
}
