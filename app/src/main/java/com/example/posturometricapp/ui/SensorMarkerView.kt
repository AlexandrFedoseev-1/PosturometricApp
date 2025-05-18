package com.example.posturometricapp.ui

import android.content.Context
import android.widget.TextView
import com.example.posturometricapp.R
import com.example.posturometricapp.convertToGrams
import com.example.posturometricapp.domain.model.SensorData
import com.example.posturometricapp.ui.sessionDetails.SessionDetailsViewModel
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlin.math.abs
import kotlin.math.roundToInt

class SensorMarkerView(
    context: Context,
    layoutResource: Int,
    private val entries: List<SensorData>,
    private val chartMode: () -> SessionDetailsViewModel.ChartMode,
    private val selectedSensorId: () -> Int?
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val xOffset = e.x.toLong()
        val baseTimestamp = entries.firstOrNull()?.timestamp ?: 0L
        val clickedTimestamp = baseTimestamp + xOffset * 3000
        val idx = e.x.toInt().coerceIn(entries.indices)
        val data = entries[idx]

//        val nearest = entries.minByOrNull { abs(it.timestamp - clickedTimestamp) }
//        nearest?.let { data ->
//            val valueText = when (chartMode()) {
//                SessionDetailsViewModel.ChartMode.TEMPERATURE -> "Темп: %.1f°C".format(data.temperature)
//                SessionDetailsViewModel.ChartMode.SENSOR -> {
//                    val sensorId = selectedSensorId() ?: 0
//                    "#${sensorId + 1}: ${data.sensorValues[sensorId]}"
//                }
//            }
//            tvContent.text = valueText
//        }
        val valueText = when (chartMode()) {
            SessionDetailsViewModel.ChartMode.TEMPERATURE ->
                "Темп: %.1f°C".format(data.temperature)
            SessionDetailsViewModel.ChartMode.SENSOR -> {
                val sensorId = selectedSensorId() ?: 0
                "№${sensorId + 1}: ${convertToGrams(data.sensorValues[sensorId].toFloat())}"
            }
        }
        tvContent.text = valueText

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat() , -height.toFloat() )
    }
}