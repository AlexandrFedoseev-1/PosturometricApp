package com.example.posturometricapp.ui

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posturometricapp.FakeUsbSensorDataSource
import com.example.posturometricapp.domain.api.SensorDataInteractor
import com.example.posturometricapp.domain.model.SensorData
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel для работы с данными датчиков.
 * Получает live-данные через SensorDataInteractor и преобразовывает их для UI.
 */
class SensorViewModel(private val sensorDataInteractor: SensorDataInteractor): ViewModel() {

    private val _sensorData = MutableLiveData<SensorData>()
    val sensorData: LiveData<SensorData> get() = _sensorData

    // Job для управления сбором данных из потока
    private var sensorDataJob: Job? = null

    /**
     * Запускает сбор live-данных от датчиков.
     */
    fun startLiveData() {
        sensorDataJob = viewModelScope.launch {
            sensorDataInteractor.getLiveSensorData().collect { data ->
                _sensorData.value = data
            }
        }
    }

    /**
     * Останавливает сбор live-данных.
     */
    fun stopLiveData() {
        sensorDataJob?.cancel()
    }

    /**
     * Отправляет команду включения считывания.
     */
    fun enableReading() {
        viewModelScope.launch {
            sensorDataInteractor.sendEnableReadingCommand()
        }
    }

    /**
     * Отправляет команду остановки считывания.
     */
    fun stopReading() {
        viewModelScope.launch {
            sensorDataInteractor.sendStopReadingCommand()
        }
    }

    /**
     * Отправляет команду запуска калибровки.
     */
    fun calibrate() {
        viewModelScope.launch {
            sensorDataInteractor.sendCalibrationCommand()
        }
    }
    /**
     * Отправляет команду калибровки и (опционально) сохраняет вычисленные offset'ы.
     */
    fun calibrateSensors() {
        viewModelScope.launch {
            val result = sensorDataInteractor.calibrateSensors()
            if (result) {
                Log.d("Calibration", "Sensors calibrated successfully.")
            } else {
                Log.e("Calibration", "Calibration failed.")
            }
        }
    }


    /**
     * Преобразует значение датчика в цвет для UI.
     *
     * Например:
     * - Если значение ниже THRESHOLD_LOW – зеленый (давление нормальное)
     * - Если значение выше THRESHOLD_HIGH – красный (давление высокое)
     * - Иначе – желтый (предварительный уровень)
     */
    fun getColorForValue(value: Long): Int {
        return when {
            value < THRESHOLD_LOW -> Color.GREEN
            value > THRESHOLD_HIGH -> Color.RED
            else -> Color.YELLOW
        }
    }

    companion object {
        // Пороговые значения для определения цвета датчика (примерные значения)
        const val THRESHOLD_LOW = 3000.0f
        const val THRESHOLD_HIGH = 4000.0f
    }
}