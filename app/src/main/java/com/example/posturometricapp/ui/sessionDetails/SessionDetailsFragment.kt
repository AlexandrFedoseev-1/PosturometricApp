package com.example.posturometricapp.ui.sessionDetails

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.posturometricapp.R
import com.example.posturometricapp.databinding.FragmentSessionDetailsBinding
import com.example.posturometricapp.databinding.FragmentSessionListBinding
import com.example.posturometricapp.formatTimestampTime
import com.example.posturometricapp.ui.SensorMarkerView
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.viewModel


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
        viewModel.sensorRecords.observe(viewLifecycleOwner) { records ->
            if (records.isEmpty()) return@observe
            val marker = SensorMarkerView(
                context = requireContext(),
                layoutResource = R.layout.marker_sensor,
                entries = records,
                chartMode = { viewModel.chartMode.value ?: SessionDetailsViewModel.ChartMode.SENSOR },
                selectedSensorId = { viewModel.selectedSensor.value }
            )
            binding.lineChart.marker = marker
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
        }// После того, как вы настроили chart (isDragEnabled, isHighlightPerDragEnabled и т.д.)




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