package com.example.meteo

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeValueMarker(
    context: Context,
    layoutResId: Int,
    private val timestamps: List<Long>,
    private val metric: ChartMetric,
    private val fullFormatter: SimpleDateFormat
) : MarkerView(context, layoutResId) {

    private val tvContent: TextView = findViewById(R.id.tvMarkerContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val index = e.x.toInt().coerceIn(0, timestamps.size - 1)
        val date = Date(timestamps[index])
        val dateStr = fullFormatter.format(date)

        val valueStr = when (metric) {
            ChartMetric.TEMPERATURE ->
                String.format(Locale.getDefault(), "%.1f °C", e.y)
            ChartMetric.HUMIDITY ->
                String.format(Locale.getDefault(), "%.1f %%", e.y)
        }

        tvContent.text = "$valueStr\n$dateStr"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Centra el marcador sobre el punto y lo coloca algo por encima
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
