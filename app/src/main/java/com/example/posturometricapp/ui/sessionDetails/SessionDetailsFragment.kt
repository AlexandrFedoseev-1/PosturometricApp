package com.example.posturometricapp.ui.sessionDetails

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.github.mikephil.charting.components.YAxis
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
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale
import kotlin.math.absoluteValue


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
        viewModel.getDataForSession(args.session.id)
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
        installStateShading()
        observeDataAndRedraw()
        viewModel.chartEntries.observe(viewLifecycleOwner) { entries ->
            updateChart(entries)
        }
        viewModel.sensorRecords.observe(viewLifecycleOwner) { recs ->
            if (recs.isEmpty()) return@observe
            val marker = SensorMarkerView(
                context = requireContext(),
                layoutResource = R.layout.marker_sensor,
                entries = recs,
                chartMode = {
                    viewModel.chartMode.value ?: SessionDetailsViewModel.ChartMode.SENSOR
                },
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
                        val deltaMs = recs[idx].timestamp - (recs.firstOrNull()?.timestamp ?: 0L)
                        val seconds = deltaMs / 1000
                        val ms = (deltaMs % 1000) / 100
                        // Например: "12.3s"
                        binding.tvSliderTime.text = "${seconds}.${ms}s"
                        viewModel.onSliderIndexChanged(idx)
                    }
                }

                // отрисуем сразу для позиции 0
                updateBarChart(0, recs)
                viewModel.onSliderIndexChanged(0)
                binding.tvSliderTime.text = "0.0s"
            }
        }// После того, как вы настроили chart (isDragEnabled, isHighlightPerDragEnabled и т.д.)
        binding.lineChart.setOnTouchListener { v, event ->
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
        binding.barChart.setOnTouchListener { v, event ->
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

        viewModel.currentPsychState.observe(viewLifecycleOwner) { stateName ->
            binding.tvPsychState.text = "Состояние: $stateName"
        }

        // 6) Stats
        viewModel.selectedSensorStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.cardStats.visibility = View.VISIBLE
                binding.tvMaxStat.text =
                    "Max: ${stats.maxValue} - Time: ${SessionDetailsViewModel.formatTime(stats.maxTime)}"
                binding.tvMinStat.text =
                    "Min: ${stats.minValue} - Time: ${SessionDetailsViewModel.formatTime(stats.minTime)}"
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
                    val idx = value.toInt()
                        .coerceIn(0, viewModel.sensorRecords.value?.size?.minus(1) ?: 0)
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

    // Логика для цветных областей (состояний)
    private fun installStateShading() {
        val chart = binding.lineChart

        chart.renderer = object : LineChartRenderer(chart, chart.animator, chart.viewPortHandler) {
            private val paint = Paint().apply { style = Paint.Style.FILL }

            override fun drawExtras(c: Canvas) {
                // Сначала рисуем области
                val recs = viewModel.sensorRecords.value.orEmpty()
                val states = viewModel.psychStates.value.orEmpty()
                if (recs.isNotEmpty() && states.isNotEmpty()) {
                    val transformer = chart.getTransformer(YAxis.AxisDependency.LEFT)
                    states.forEach { state ->
                        // 1) Находим индексы первой и последней записи в этом состоянии
                        val startIdx = recs.indexOfFirst { it.timestamp >= state.startTime }
                            .takeIf { it >= 0 } ?: return@forEach
                        val endIdx = recs.indexOfLast {
                            it.timestamp <= (state.endTime ?: Long.MAX_VALUE)
                        }.takeIf { it >= 0 } ?: return@forEach

                        // 2) Преобразуем их в экранные координаты по X
                        val pts = floatArrayOf(startIdx.toFloat(), 0f, endIdx.toFloat(), 0f)
                        transformer.pointValuesToPixel(pts)
                        val left = pts[0]
                        val right = pts[2]

                        // 3) Рисуем полупрозрачный прямоугольник от top до bottom графика
                        paint.color = getColorForState(state.stateName)
                        val top = chart.viewPortHandler.contentTop()
                        val bottom = chart.viewPortHandler.contentBottom()
                        c.drawRect(left, top, right, bottom, paint)
                    }
                }

                // Затем рисуем всё остальное (сетки, графики, т.д.)
                super.drawExtras(c)
            }
        }
    }

    private val stateColorMap = mutableMapOf<String, Int>()

    private fun getColorForState(name: String): Int {
        return stateColorMap.getOrPut(name) {
            // Здесь можно выбрать палитру или хеширование
            val baseColors = listOf(
                Color.parseColor("#66BB6A"), // зелёный
                Color.parseColor("#EF5350"), // красный
                Color.parseColor("#42A5F5"), // синий
                Color.parseColor("#FFCA28"), // жёлтый
                Color.parseColor("#AB47BC")  // фиолетовый
            )
            // Берём цвет из палитры по хешу имени
            val idx = (name.hashCode().absoluteValue) % baseColors.size
            // Делаем полупрозрачным
            baseColors[idx].withAlpha(30)

            if (name =="Спокойный"){
                baseColors[0].withAlpha(30)
            }else if (name =="Напряженный"){
                baseColors[1].withAlpha(30)
            }else if (name =="Встревоженный"){
                baseColors[3].withAlpha(30)
            }else{
                baseColors[4].withAlpha(30)
            }
        }
    }

    private fun observeDataAndRedraw() {
        viewModel.sensorRecords.observe(viewLifecycleOwner) {
            binding.lineChart.invalidate()
        }
        viewModel.psychStates.observe(viewLifecycleOwner) {
            binding.lineChart.invalidate()
        }
    }

    // Расширение для простого изменения альфа-канала
    private fun Int.withAlpha(alpha: Int): Int {
        val a = (alpha.coerceIn(0, 255) shl 24)
        val rgb = this and 0x00FFFFFF
        return a or rgb
    }
}