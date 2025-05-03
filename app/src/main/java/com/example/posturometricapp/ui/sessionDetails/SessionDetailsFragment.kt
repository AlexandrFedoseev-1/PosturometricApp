package com.example.posturometricapp.ui.sessionDetails

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.posturometricapp.R
import com.example.posturometricapp.databinding.FragmentSessionDetailsBinding
import com.example.posturometricapp.databinding.FragmentSessionListBinding
import com.example.posturometricapp.domain.model.SensorData
import com.example.posturometricapp.formatTimestampTime
import com.example.posturometricapp.ui.BarMarkerView
import com.example.posturometricapp.ui.SensorMarkerView
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale


class SessionDetailsFragment : Fragment() {
    private val args: SessionDetailsFragmentArgs by navArgs()
    private val viewModel by viewModel<SessionDetailsViewModel>()
    private var _binding: FragmentSessionDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var topAdapter: TopSensorsAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getSensorDataForSession(args.session.id)
        binding.tvStart.text = "Начало:" + formatTimestampTime(args.session.startTime)
        binding.tvEnd.text = "Конец:" + args.session.endTime?.let { formatTimestampTime(it) }
        // 1) Настройка Top 6
        topAdapter = TopSensorsAdapter { sensorId ->
            viewModel.onTopSensorClicked(sensorId)
            // свернуть chipGroup, снять выделение
            binding.chipGroupSensors.clearCheck()
        }
        binding.rvTopSensors.adapter = topAdapter

        viewModel.topSensors.observe(viewLifecycleOwner) {
            topAdapter.submitList(it)
        }

        // 2) Кнопка Температуры
        binding.btnTemperature.setOnClickListener {
            viewModel.onTemperatureClicked()
            binding.chipGroupSensors.clearCheck()
        }
        viewModel.averageTemperature.observe(viewLifecycleOwner) { avg ->
            binding.btnTemperature.text = "Темп: ${"%.1f".format(avg)}°C"
        }

        // 3) Генерация chip'ов 32 датчика
        binding.chipGroupSensors.removeAllViews()
        repeat(32) { idx ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = "#${idx + 1}"
                isCheckable = true
                setOnClickListener { viewModel.onSensorChipSelected(idx) }
            }
            binding.chipGroupSensors.addView(chip)
        }

        // 4) Chart initialization
        sensorChartSetup()
        viewModel.chartEntries.observe(viewLifecycleOwner) { entries ->
            updateChart(entries)
        }
        viewModel.sensorRecords.observe(viewLifecycleOwner) { recs ->
            if (recs.isEmpty()) return@observe
            val marker = SensorMarkerView(
                context = requireContext(),
                layoutResource = R.layout.marker_sensor,
                entries = recs,
                chartMode = { viewModel.chartMode.value ?: SessionDetailsViewModel.ChartMode.SENSOR },
                selectedSensorId = { viewModel.selectedSensor.value }
            )
            binding.lineChart.marker = marker

            // 2) Настроить Slider + BarChart
            if (recs.size < 2) {
                binding.sliderTime.isVisible = false
                updateBarChart(0, recs)
                binding.tvSliderTime.text = "0.0s"
            } else {
                binding.sliderTime.apply {
                    isVisible = true
                    valueFrom = 0f
                    valueTo = (recs.size - 1).toFloat()
                    stepSize = 1f
                    value = 0f
                    addOnChangeListener { _, value, _ ->
                        val idx = value.toInt()
                        // 1) обновляем гистограмму
                        updateBarChart(idx, recs)
                        // 2) обновляем текст сбоку
//                        val deltaMs = recs[idx].timestamp - recs.first().timestamp
//                        val seconds = deltaMs / 1000f
                        val deltaMs = recs[idx].timestamp - (recs.firstOrNull()?.timestamp ?: 0L)
                        val seconds = deltaMs / 1000
                        val ms = (deltaMs % 1000) / 100
                        // Например: "12.3s"
                        binding.tvSliderTime.text =  "${seconds}.${ms}"
                    }
                }

                // отрисуем сразу для позиции 0
                updateBarChart(0, recs)
                binding.tvSliderTime.text = "0.0s"
            }
        }// После того, как вы настроили chart (isDragEnabled, isHighlightPerDragEnabled и т.д.)
        binding.lineChart.setOnTouchListener{ v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
        binding.barChart.setOnTouchListener{ v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }


        // 6) Stats
        viewModel.selectedSensorStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.cardStats.visibility = View.VISIBLE
                binding.tvMaxStat.text = "Max: ${stats.maxValue} - Time: ${SessionDetailsViewModel.formatTime(stats.maxTime)}"
                binding.tvMinStat.text = "Min: ${stats.minValue} - Time: ${SessionDetailsViewModel.formatTime(stats.minTime)}"
                binding.tvAvgStat.text = "Avg: ${"%.1f".format(stats.average)}"
            } else {

            }
        }

    }
    private fun updateBarChart(index: Int, recs: List<SensorData>) {
        // получаем именно тот замер
        val values = recs[index].sensorValues

        // создаём BarEntry для всех 32 сенсоров
        val entries = values.mapIndexed { sensorId, v ->
            BarEntry(sensorId.toFloat(), v.toFloat())
        }

        val set = BarDataSet(entries, "Давление по сенсорам").apply {
            setDrawValues(false)
        }
        val data = BarData(set).apply {
            barWidth = 0.8f
        }

        binding.barChart.apply {
            this.data = data
            description.isEnabled = false
            axisRight.isEnabled = false
            val marker = BarMarkerView(requireContext(), R.layout.marker_sensor)
            binding.barChart.marker = marker
            // X-ось: подписи «№1, №2, … №32»
            xAxis.apply {
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter((1..32).map { "#$it" })
                position = XAxis.XAxisPosition.BOTTOM
            }

            axisLeft.axisMinimum = 0f  // если хотим от нуля
            // Включаем реакцию на тапы
            isHighlightPerTapEnabled = true

            invalidate()
        }
    }
    private fun sensorChartSetup() {
        with(binding.lineChart) {
            description.isEnabled = false
            axisRight.isEnabled = false
            legend.isEnabled = false

            // Границы X
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true

            isDragEnabled = true
            setScaleEnabled(true)
            isHighlightPerDragEnabled = true
            // formatter: берём индекс, находим timestamp в наших записях и форматируем в s, ms
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val idx = value.toInt().coerceIn(0, viewModel.sensorRecords.value?.size?.minus(1) ?: 0)
                    val recs = viewModel.sensorRecords.value ?: return ""
                    // смещение в секундах
                    val deltaMs = recs[idx].timestamp - (recs.firstOrNull()?.timestamp ?: 0L)
                    val seconds = deltaMs / 1000
                    val ms = (deltaMs % 1000) / 100
                    // Например: "12.3s"
                    return "${seconds}.${ms}"
                }
            }
        }
    }

    private fun updateChart(entries: List<Entry>) {
        val ds = LineDataSet(entries, "").apply {
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        binding.lineChart.data = LineData(ds)
        binding.lineChart.invalidate()
    }
}