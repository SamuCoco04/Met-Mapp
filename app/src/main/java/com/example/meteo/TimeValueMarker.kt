package com.example.meteo

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class TimeValueMarker(
    context: Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvMarkerValue: TextView = findViewById(R.id.tvMarkerValue)
    private val tvMarkerTime: TextView = findViewById(R.id.tvMarkerTime)

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) {
            super.refreshContent(e, highlight)
            return
        }

        // Valor REAL: si lo hemos guardado en data lo usamos, si no, usamos e.y
        val rawValue = (e.data as? Double) ?: e.y.toDouble()
        val valueStr = String.format(Locale.getDefault(), "%.2f", rawValue)

        val timeMillis = e.x.toLong()
        val timeStr = dateFormat.format(Date(timeMillis))

        // Etiqueta de la serie (Temperatura, Humedad, Partículas…)
        val label = highlight?.dataSetIndex?.let { dsIndex ->
            chartView?.data?.getDataSetByIndex(dsIndex)?.label
        } ?: ""

        tvMarkerValue.text = "$label: $valueStr"
        tvMarkerTime.text = timeStr

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // centramos el marker encima del punto
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
