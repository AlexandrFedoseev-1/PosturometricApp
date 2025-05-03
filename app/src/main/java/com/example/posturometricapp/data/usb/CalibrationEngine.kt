package com.example.posturometricapp.data.usb

/**
 * Движок калибровки: собирает N образцов, вычисляет offsets и применяет их.
 */
class CalibrationEngine(
    private val requiredSamples: Int = 10
) {
    private val samples = mutableListOf<List<Long>>()
    private var _offsets: List<Long>? = null
    val offsets: List<Long>? get() = _offsets

    /**
     * Добавляет один образец. Возвращает true, когда offsets готовы.
     */
    fun addSample(values: List<Long>): Boolean {
        samples.add(values)
        return if (samples.size >= requiredSamples) {
            computeOffsets()
            true
        } else false
    }

    /**
     * Применяет ранее вычисленные offsets к списку значений.
     */
    fun applyCalibration(values: List<Long>): List<Long> {
        val offs = _offsets ?: return values
        return values.mapIndexed { i, v -> v - offs[i] }
    }

    /**
     * Сбрасывает состояние калибровки.
     */
    fun reset() {
        samples.clear()
        _offsets = null
    }

    private fun computeOffsets() {
        val count = samples.first().size
        _offsets = List(count) { idx ->
            samples.map { it[idx] }.average().toLong()
        }
    }
}