package com.example.posturometricapp.data.usb

/**
 * Модель данных, получаемых с кресла.
 * sensorValues – список из 32 значений датчиков,
 * temperature – значение температуры,
 * timestamp – время получения данных.
 */

data class SensorDataUsb(
    val sensorValues: List<Long>,
    val temperature: Double,
    val timestamp: Long = System.currentTimeMillis()
)