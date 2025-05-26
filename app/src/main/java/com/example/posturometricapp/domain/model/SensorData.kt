package com.example.posturometricapp.domain.model

/**
 * Доменная модель данных от датчиков.
 *
 * @param sensorValues список значений 32 датчиков
 * @param temperature значение температуры
 * @param timestamp время получения данных
 */
data class SensorData(
    val sensorValues: List<Long>,
    val temperature: Double,
    val timestamp: Long = System.currentTimeMillis()
)