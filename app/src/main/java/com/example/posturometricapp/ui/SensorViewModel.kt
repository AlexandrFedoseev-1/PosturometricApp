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
import com.example.posturometricapp.domain.model.SensorButtonState
import com.example.posturometricapp.domain.model.SensorData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _screenState = MutableLiveData<SensorButtonState>(SensorButtonState.Empty)
    val screenState: LiveData<SensorButtonState> get() = _screenState

    private val _message = MutableLiveData<String>("")
    val message: LiveData<String> get() = _message

    private val _psychState = MutableLiveData<String>("")
    val psychState: LiveData<String> get() = _psychState

    // Job для управления сбором данных из потока
    private var sensorDataJob: Job? = null

    private var playbackJob: Job? = null

    // Текущий sessionId, полученный при запуске сеанса
    private var currentSessionId: Long? = null

    // Текущий psychStateId
    private var currentPsychStateId: Long? = null
    var con = 0

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
            currentPsychStateId = psychStateInteractor.startState(currentSessionId!!, stateName)
            _screenState.value = SensorButtonState.StartSession
        }
        _psychState.value = stateName
        _message.value = "Запись сессии начата!"
    }

    /**
     * Завершает сеанс.
     * Вызывает stopSession() у репозитория и останавливает сбор live-данных.
     */

    fun stopSession() {
        viewModelScope.launch {
            // Завершаем текущее псих. состояние
            currentPsychStateId?.let { psychStateInteractor.endState(it) }
            // Завершаем сессию и поток данных
            currentSessionId?.let { sensorDataInteractor.stopSession(it) }
            sensorDataJob?.cancel()
            playbackJob?.cancel()
            currentSessionId = null
            currentPsychStateId = null
            _screenState.value = SensorButtonState.HasPermission
            _message.value = "Сессия завершена!"
            _psychState.value = ""
        }
    }

    fun switchPsychState(stateName: String) {
        viewModelScope.launch {
            currentSessionId?.let {
                currentPsychStateId = psychStateInteractor.switchState(it, stateName)
            }
        }
        _psychState.value = stateName
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
                if (con ==0){
                    delay(2000L)
                    calibrateSensors()
                    con = 1
                }
            }
        }
        _message.value = "Запущен поток данных!"
        _screenState.value = SensorButtonState.StreamData

    }


    /**
     * Останавливает сбор live-данных.
     */
    fun stopLiveData() {
        if (currentSessionId != null) {
            stopSession()
        } else {
            sensorDataJob?.cancel()
            _screenState.value = SensorButtonState.HasPermission
            _message.value = "Поток данных остановлен!"
        }

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
        _message.value = "Запущен поток данных!"
        _screenState.value = SensorButtonState.StreamData
    }

    fun stopPlayback() {
        if (currentSessionId != null) {
            stopSession()
        } else {
            playbackJob?.cancel()
            _screenState.value = SensorButtonState.HasPermission
            _message.value = "Поток данных остановлен!"
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
            value > THRESHOLD_LOW && value < THRESHOLD_NEGATIVE -> Color.GREEN
            value < THRESHOLD_HIGH -> Color.RED
            value > THRESHOLD_NEGATIVE -> Color.BLUE
            else -> Color.YELLOW
        }
    }
    fun fakePermissions(){
        _message.value = "Успешное подключение!"
        _screenState.value = SensorButtonState.HasPermission
    }
    fun clearMessage() {
        _message.value = ""
    }

    fun setScreenState(state: SensorButtonState){
        _screenState.value = state
    }


    companion object {
        // Пороговые значения для определения цвета датчика (примерные значения)
        const val THRESHOLD_LOW = -10000.0f * 100
        const val THRESHOLD_HIGH = -50000.0f * 100
        const val THRESHOLD_NEGATIVE = 10000.0f * 100

    }
}