package com.example.posturometricapp.ui

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posturometricapp.SessionPlaybackSensorDataSource
import com.example.posturometricapp.domain.api.PsychStateInteractor
import com.example.posturometricapp.domain.api.SensorDataInteractor
import com.example.posturometricapp.domain.model.SensorData
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewModel для работы с данными датчиков.
 * Получает live-данные через SensorDataInteractor и преобразовывает их для UI.
 */
class SensorViewModel(
    private val sensorDataInteractor: SensorDataInteractor,
    private val psychStateInteractor: PsychStateInteractor,
    private val playbackSource: SessionPlaybackSensorDataSource
) : ViewModel() {

    private val _sensorData = MutableLiveData<SensorData>()
    val sensorData: LiveData<SensorData> get() = _sensorData

    // Job для управления сбором данных из потока
    private var sensorDataJob: Job? = null

    private var playbackJob: Job? = null

    // Текущий sessionId, полученный при запуске сеанса
    private var currentSessionId: Long? = null

    // Текущий psychStateId
    private var currentStateId: Long? = null

    /**
     * Начинает сеанс. Вызывает startSession() у репозитория (use case),
     * сохраняет sessionId и запускает прослушивание live-данных.
     */
    fun startSession() {
        viewModelScope.launch {
            currentSessionId = sensorDataInteractor.startSession()
            Log.d("Session", "Session started with id: $currentSessionId")
//            startLiveData() // Можно объединить с запуском live данных
        }
    }

    fun startSessionWithPsychState(stateName: String) {
        viewModelScope.launch {
            currentSessionId = sensorDataInteractor.startSession()
            currentStateId = psychStateInteractor.startState(currentSessionId!!, stateName)
        }
    }

    /**
     * Завершает сеанс.
     * Вызывает stopSession() у репозитория и останавливает сбор live-данных.
     */

    fun stopSession() {
        viewModelScope.launch {
            // Завершаем текущее псих. состояние
            currentStateId?.let { psychStateInteractor.endState(it) }
            // Завершаем сессию и поток данных
            currentSessionId?.let { sensorDataInteractor.stopSession(it) }
            sensorDataJob?.cancel()
            currentSessionId = null
            currentStateId = null
        }
    }

    fun switchPsychState(stateName: String) {
        viewModelScope.launch {
            currentSessionId?.let {
                currentStateId = psychStateInteractor.switchState(it, stateName)
            }
        }
    }

    /**
     * Запускает сбор live-данных от датчиков.
     * В дополнение к обновлению LiveData, каждая полученная запись сохраняется в БД
     * с привязкой к текущему sessionId.
     */
    fun startLiveData() {
        sensorDataJob = viewModelScope.launch {
            sensorDataInteractor.getLiveSensorData().collect { data ->
                // Обновляем LiveData для UI
                _sensorData.value = data

                // Если сеанс активен, сохраняем запись в БД
                currentSessionId?.let { sessionId ->
                    sensorDataInteractor.saveSensorData(sessionId, data)
                }
            }
        }
    }

    /**
     * Останавливает сбор live-данных.
     */
    fun stopLiveData() {
        sensorDataJob?.cancel()
        Log.d("Session", "Session stopped with id: ${_sensorData.value?.sensorValues}")
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


    /** Запуск воспроизведения записи из БД */
    fun playSession(sessionId: Long) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            playbackSource.startPlayback(sessionId).collect { data ->
                _sensorData.value = data
                // Если сеанс активен, сохраняем запись в БД
                currentSessionId?.let { sessionId ->
                    sensorDataInteractor.saveSensorData(sessionId, data)
                }
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
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
            value > THRESHOLD_LOW && value < THRESHOLD_NEGATIVE -> Color.GREEN
            value < THRESHOLD_HIGH -> Color.RED
            value > THRESHOLD_NEGATIVE -> Color.BLUE
            else -> Color.YELLOW
        }
    }

    companion object {
        // Пороговые значения для определения цвета датчика (примерные значения)
        const val THRESHOLD_LOW = -10000.0f * 100
        const val THRESHOLD_HIGH = -50000.0f * 100
        const val THRESHOLD_NEGATIVE = 10000.0f * 100

    }
}