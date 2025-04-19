package com.example.posturometricapp.data.repository

import com.example.posturometricapp.data.dp.AppDatabase
import com.example.posturometricapp.data.dp.dao.SensorDataDao
import com.example.posturometricapp.data.dp.dao.SessionDao
import com.example.posturometricapp.data.dp.entity.SensorDataEntity
import com.example.posturometricapp.data.dp.entity.SessionEntity
import com.example.posturometricapp.data.usb.UsbSensorDataSource
import com.example.posturometricapp.domain.model.SensorData
import com.example.posturometricapp.domain.api.SensorDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Реализация репозитория для работы с данными датчиков.
 * Помимо получения и сохранения данных, теперь инкапсулирует логику отправки команд Arduino.
 */
class SensorDataRepositoryImpl(
    private val usbSensorDataSource: UsbSensorDataSource,
    private val appDatabase: AppDatabase
) : SensorDataRepository {

    /**
     * Запускает сеанс, создавая новую запись в таблице session.
     * Возвращает идентификатор сеанса.
     */
    override suspend fun startSession(): Long {
        val sessionEntity = SessionEntity(startTime = System.currentTimeMillis())
        return appDatabase.sessionDao().insertSession(sessionEntity)
    }

    /**
     * Завершает сеанс, обновляя время завершения.
     */
    override suspend fun stopSession(sessionId: Long) {
        appDatabase.sessionDao().updateSessionEndTime(sessionId, System.currentTimeMillis())
    }

    /**
     * Предоставляет поток live-данных с датчиков.
     * При получении данных с USB происходит маппинг в доменную модель SensorDataUsb.
     */
    override fun getLiveSensorData(): Flow<SensorData> {
        return usbSensorDataSource.startListening().map { usbData ->
            // Преобразуем из модели data.usb.SensorDataUsb в доменную модель SensorData.
            SensorData(
                sensorValues = usbData.sensorValues,
                temperature = usbData.temperature,
                timestamp = usbData.timestamp
            )
        }
    }

    /**
     * Сохраняет данные от датчиков в базу, привязывая их к активному сеансу.
     */
    override suspend fun saveSensorData(sessionId: Long, sensorData: SensorData) {
        val sensorDataEntity = SensorDataEntity(
            sessionId = sessionId,
            timestamp = sensorData.timestamp,
            sensorValues = sensorData.sensorValues,
            temperature = sensorData.temperature
        )
        appDatabase.sensorDao().insertSensorData(sensorDataEntity)
    }

    /**
     * Калибровка внутри приложения. Делает значения датчиков приближенными к нулю в состоянии покоя.
     */
    override suspend fun calibrateSensors(): Boolean {
        return usbSensorDataSource.calibrateSensors()
    }

    /**
     * Отправляет команду для включения считывания (на Arduino: "CR#").
     */
    override suspend fun sendEnableReadingCommand(): Boolean {
        return usbSensorDataSource.sendCommand("CR#")
    }

    /**
     * Отправляет команду для остановки считывания (на Arduino: "CS#").
     */
    override suspend fun sendStopReadingCommand(): Boolean {
        return usbSensorDataSource.sendCommand("CS#")
    }

    /**
     * Отправляет команду для запуска калибровки (на Arduino: "CU#").
     */
    override suspend fun sendCalibrationCommand(): Boolean {
        return usbSensorDataSource.sendCommand("CU#")
    }
}