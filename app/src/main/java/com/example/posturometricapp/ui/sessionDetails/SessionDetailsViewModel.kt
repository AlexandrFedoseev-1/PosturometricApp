package com.example.posturometricapp.ui.sessionDetails

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.posturometricapp.domain.api.PsychStateInteractor

import com.example.posturometricapp.domain.api.SessionInteractor
import com.example.posturometricapp.domain.model.PsychState
import com.example.posturometricapp.domain.model.SensorData
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class SessionDetailsViewModel(
    private val interactor: SessionInteractor,
    private val psychStateInteractor: PsychStateInteractor
) : ViewModel() {
    // 1️⃣ Session metadata
//    val session = interactor.observeSession(sessionId).asLiveData()

    // 2️⃣ Full sensor records
    private val sensorData = MutableLiveData<List<SensorData>>()
    val sensorRecords: LiveData<List<SensorData>> = sensorData
    private val emaValues = MutableList(32) { 0.0 }
    private val lastValues = MutableList(32) { 0L }
    private val history = Array(32) { mutableListOf<Long>() }


    private val _psychStates = MutableLiveData<List<PsychState>>()
    val psychStates: LiveData<List<PsychState>> = _psychStates

    private val _currentPsychState = MutableLiveData<String>()
    val currentPsychState: LiveData<String> = _currentPsychState
    // Параметры
    private val alpha = 0.2
    private val rateLimit = 10_000_000L
    private val outlierZThreshold = 3.0
    private val historySize = 30

    fun onSliderIndexChanged(idx: Int) {
        val recs = sensorRecords.value.orEmpty()
        val states = psychStates.value.orEmpty()


        if (recs.isEmpty() || states.isEmpty()) {
            _currentPsychState.value = "–"
            return
        }
        val ts = recs.getOrNull(idx)?.timestamp ?: return

        // ищем состояние, у которого start ≤ ts ≤ end
        _currentPsychState.value = states
            .find { it.startTime <= ts && ts <= it.endTime!! }
            ?.stateName
            ?: "–"
    }

    private fun filterOutliers(values: List<Long>): List<Long> {
        return values.mapIndexed { idx, value ->
            val historyList = history[idx]
            if (historyList.size < 30) {
                historyList.add(value)
                return@mapIndexed value
            }

            val mean = historyList.average()
            val std = sqrt(historyList.map { (it - mean).toDouble().pow(2) }.average())
            val z = if (std == 0.0) 0.0 else (value - mean) / std

            val filtered = if (abs(z) > outlierZThreshold) lastValues[idx] else value
            updateHistory(idx, filtered)
            filtered
        }
    }

    fun filterOutliers(sensorValues: Map<Int, List<Long>>): Map<Int, List<Long>> {
        return sensorValues.mapValues { (_, values) ->
            val mean = values.average()
            val std = sqrt(
                values.map { (it - mean).toDouble().pow(2) }.average()
                    .coerceAtLeast(1.0)
            ) // защита от деления на 0

            values.map { value ->
                val z = (value - mean) / std
                if (abs(z) > outlierZThreshold) mean.toLong() else value
            }
        }
    }


    private fun updateHistory(idx: Int, value: Long) {
        val list = history[idx]
        list.add(value)
        if (list.size > historySize) {
            list.removeAt(0)
        }
    }

    private fun filterRateLimit(values: List<Long>): List<Long> {
        return values.mapIndexed { idx, value ->
            val last = lastValues[idx]
            val diff = value - last

            val limited = when {
                abs(diff) <= rateLimit -> value
//                diff > 0 -> last + rateLimit
//                else -> last - rateLimit
                else -> lastValues[idx]
            }

            lastValues[idx] = limited
            limited
        }
    }

    fun filterRateLimit(sensorValues: Map<Int, List<Long>>): Map<Int, List<Long>> {
        return sensorValues.mapValues { (_, values) ->
            if (values.isEmpty()) return@mapValues emptyList()

            val limited = mutableListOf<Long>()
            limited.add(values[0])

            for (i in 1 until values.size) {
                val prev = limited.last()
                val delta = values[i] - prev
                val clamped = when {
                    abs(delta) <= rateLimit -> values[i]
                    delta > 0 -> prev + rateLimit
                    else -> prev - rateLimit
                }
                limited.add(clamped)
            }

            limited
        }
    }


    private fun filterEma(values: List<Long>): List<Long> {
        return values.mapIndexed { idx, value ->
            val smoothed = alpha * value + (1 - alpha) * emaValues[idx]
            emaValues[idx] = smoothed
            smoothed.toLong()
        }
    }

    fun filterEma(sensorValues: Map<Int, List<Long>>): Map<Int, List<Long>> {
        return sensorValues.mapValues { (_, values) ->
            if (values.isEmpty()) return@mapValues emptyList()

            val smoothed = mutableListOf<Double>()
            smoothed.add(values[0].toDouble())

            for (i in 1 until values.size) {
                val ema = alpha * values[i] + (1 - alpha) * smoothed.last()
                smoothed.add(ema)
            }

            smoothed.map { it.toLong() }
        }
    }

    fun getDataForSession(sessionId: Long) {
        viewModelScope.launch {
            interactor.getSensorDataForSession(sessionId).collect { list ->
                val fixed = list.map { data ->
                    data.copy(sensorValues = data.sensorValues.map { -it }) // инверсия
                }
                val smoothedList = fixed.map { data ->
                    val step1 = filterOutliers(data.sensorValues)
                    val step2 = filterRateLimit(data.sensorValues)
                    val step3 = filterEma(data.sensorValues)
                    data.copy(sensorValues = step2)
                }
                sensorData.postValue(smoothedList)
            }

        }
        viewModelScope.launch {
            psychStateInteractor.getStatesForSession(sessionId).collect { list ->
                _psychStates.postValue(list)
            }
        }
    }

    // 3️⃣ Top 6 sensors by max pressure
    data class TopSensor(val id: Int, val maxValue: Long)

    val topSensors: LiveData<List<TopSensor>> = sensorData.map { recs ->
        // For each sensor index compute its maximum
        val count = recs.firstOrNull()?.sensorValues?.size ?: 0
        (0 until count)
            .map { idx -> TopSensor(idx, recs.maxOf { it.sensorValues[idx] }) }
            .sortedByDescending { it.maxValue }
            .take(6)
    }


    // 4️⃣ Average session temperature
    val averageTemperature: LiveData<Double> = sensorData.map { recs ->
        recs.map { it.temperature }.average()
    }


    // 5️⃣ Chart mode & selection
    enum class ChartMode { SENSOR, TEMPERATURE }

    private val _chartMode = MutableLiveData(ChartMode.SENSOR)
    val chartMode: LiveData<ChartMode> = _chartMode

    private val _selectedSensor = MutableLiveData<Int?>(null)
    val selectedSensor: LiveData<Int?> = _selectedSensor

    // 6️⃣ Chart entries (x=timeOffset, y=value)
    val chartEntries: LiveData<List<Entry>> = MediatorLiveData<List<Entry>>().apply {
        fun update() {
            val recs = sensorRecords.value.orEmpty()
            if (recs.isEmpty()) return
            // Use session-relative seconds as x
            val entries = when (_chartMode.value) {
                ChartMode.TEMPERATURE -> recs.mapIndexed { i, it ->
                    Entry(i.toFloat(), it.temperature.toFloat())
                }


                ChartMode.SENSOR -> {
                    val idx = _selectedSensor.value ?: 0
                    recs.mapIndexed { i, it ->
                        Entry(i.toFloat(), it.sensorValues[idx].toFloat())
                    }
                }

                else -> emptyList()
            }
            value = entries
        }
        addSource(sensorRecords) { update() }
        addSource(_chartMode) { update() }
        addSource(_selectedSensor) { update() }
    }

    // 7️⃣ Stats for selected sensor
    data class SensorStats(
        val maxValue: Long, val maxTime: Long,
        val minValue: Long, val minTime: Long,
        val average: Double
    )

    val selectedSensorStats: LiveData<SensorStats?> = _selectedSensor.switchMap { idx ->
        sensorRecords.map { recs ->
            if (idx == null || recs.isEmpty()) return@map null

            val values = recs.map { it.sensorValues[idx] }
            Log.d("sensorValues", "sensorValues[$idx]: $values ")
            val times = recs.map { it.timestamp }
            val maxPair = values.zip(times).maxByOrNull { it.first }!!
            val minPair = values.zip(times).minByOrNull { it.first }!!
            SensorStats(
                maxValue = maxPair.first,
                maxTime = maxPair.second,
                minValue = minPair.first,
                minTime = minPair.second,
                average = values.average()
            )
        }
    }

    // 8️⃣ User actions to drive UI
    fun onTopSensorClicked(sensorId: Int) {
        _chartMode.value = ChartMode.SENSOR
        _selectedSensor.value = sensorId
    }

    fun onSensorChipSelected(sensorId: Int) {
        _chartMode.value = ChartMode.SENSOR
        _selectedSensor.value = sensorId
    }

    fun onTemperatureClicked() {
        _chartMode.value = ChartMode.TEMPERATURE
        _selectedSensor.value = null
    }

    companion object {
        // Formatter for stats display
        val dateFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        fun formatTime(ms: Long) = dateFmt.format(Date(ms))
    }
}