package com.example.posturometricapp

import com.example.posturometricapp.domain.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class FakeUsbSensorDataSource {

    /**
     * Запускает генерацию фиктивных данных с датчиков.
     * Генерирует данные каждую секунду, формируя объект SensorData.
     */
    fun startListening(): Flow<SensorData> = flow {
        while (true) {
            // Генерация 32 случайных значений для датчиков
            val sensorValues = List(32) { (0..5000).random().toLong() }
            // Генерация случайной температуры от 20 до 30
            val temperature = (20..30).random().toDouble()
            // Формирование объекта данных с текущей меткой времени
            val sensorData = SensorData(sensorValues, temperature, System.currentTimeMillis())
            // Отправка объекта в Flow
            emit(sensorData)
            // Задержка между итерациями для имитации поступления данных каждую секунду
            delay(1000)
        }
    }

    /**
     * Фиктивная отправка команды.
     * Логирует вызов и возвращает true.
     *
     * @param command Команда для отправки (например, "CR#", "CS#", "CU#")
     * @return true, если команда "отправлена" успешно
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        // Логирование команды (в реальном приложении можно использовать Log.d или другую библиотеку логирования)
        println("FakeUsbSensorDataSource sendCommand: $command")
        true
    }

    /**
     * Отправляет команду включения считывания ("CR#").
     */
    suspend fun sendEnableReadingCommand(): Boolean = sendCommand("CR#")

    /**
     * Отправляет команду остановки считывания ("CS#").
     */
    suspend fun sendStopReadingCommand(): Boolean = sendCommand("CS#")

    /**
     * Отправляет команду для запуска калибровки ("CU#").
     */
    suspend fun sendCalibrationCommand(): Boolean = sendCommand("CU#")
}