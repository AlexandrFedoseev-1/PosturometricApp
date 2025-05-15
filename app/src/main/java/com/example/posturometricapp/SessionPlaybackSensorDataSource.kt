package com.example.posturometricapp

import com.example.posturometricapp.domain.api.SessionInteractor
import com.example.posturometricapp.domain.model.SensorData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Источник «фейковых» live-данных на основе уже сохранённой сессии.
 * Эмитит каждую запись с задержкой, равной разнице времён между соседними записями.
 */
class SessionPlaybackSensorDataSource(
    private val sessionInteractor: SessionInteractor
) {
    fun startPlayback(sessionId: Long): Flow<SensorData> = flow {
        // получаем все записи из сессии один раз
        val records: List<SensorData> = sessionInteractor
            .getSensorDataForSession(sessionId)
            .first()

        if (records.isEmpty()) return@flow

        // Эмитим первый элемент сразу, но с текущим временем
        emit(
            records[0].copy(
                timestamp = System.currentTimeMillis()
            )
        )

        // Для каждого следующего рассчитываем задержку и эмитим его с новым timestamp
        for (i in 1 until records.size) {
            val prevTs = records[i - 1].timestamp
            val curTs  = records[i].timestamp
            val delta  = (curTs - prevTs).coerceAtLeast(0L)

            delay(delta)

            emit(
                records[i].copy(
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}