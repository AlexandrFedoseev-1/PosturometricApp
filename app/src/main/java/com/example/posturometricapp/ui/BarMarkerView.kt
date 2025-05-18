package com.example.posturometricapp.ui

import android.content.Context
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.example.posturometricapp.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class BarMarkerView(
    context: Context,
    @LayoutRes layoutId: Int
) : MarkerView(context, layoutId) {
    private val tvValue: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is BarEntry) {
            val sensorIdx = e.x.toInt()
            val value     = e.y
            tvValue.text = "№${sensorIdx+1}: ${value}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // выше и по центру столбца
        return MPPointF(-(width/2).toFloat(), -height.toFloat())
    }
}