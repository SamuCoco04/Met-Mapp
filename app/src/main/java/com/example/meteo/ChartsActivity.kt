package com.example.meteo

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Métricas disponibles para las gráficas (visible desde TimeValueMarker)
enum class ChartMetric {
    TEMPERATURE,
    HUMIDITY
}

class ChartsActivity : AppCompatActivity() {

    private lateinit var btnTemp: Button
    private lateinit var btnHum: Button
    private lateinit var lineChart: LineChart
    private lateinit var tvChartStatus: TextView

    private val stationId = MeteoRepository.getDefaultStationId()
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    private var currentMetric: ChartMetric = ChartMetric.TEMPERATURE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        btnTemp = findViewById(R.id.btnMetricTemperature)
        btnHum = findViewById(R.id.btnMetricHumidity)
        lineChart = findViewById(R.id.lineChart)
        tvChartStatus = findViewById(R.id.tvChartStatus)

        setupChartAppearance()
        setupButtons()

        // Carga inicial
        loadDataAndShow(currentMetric, forceRefresh = false)
    }

    private fun setupButtons() {
        btnTemp.setOnClickListener {
            currentMetric = ChartMetric.TEMPERATURE
            updateMetricButtons()
            loadDataAndShow(ChartMetric.TEMPERATURE, forceRefresh = false)
        }

        btnHum.setOnClickListener {
            currentMetric = ChartMetric.HUMIDITY
            updateMetricButtons()
            loadDataAndShow(ChartMetric.HUMIDITY, forceRefresh = false)
        }

        updateMetricButtons()
    }

    private fun updateMetricButtons() {
        val selected = ContextCompat.getColor(this, R.color.accent_green)
        val unselected = ContextCompat.getColor(this, R.color.primary_green)

        btnTemp.setBackgroundColor(
            if (currentMetric == ChartMetric.TEMPERATURE) selected else unselected
        )
        btnHum.setBackgroundColor(
            if (currentMetric == ChartMetric.HUMIDITY) selected else unselected
        )
    }

    private fun setupChartAppearance() {
        lineChart.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_background))
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setNoDataText("No hay datos para mostrar")
        lineChart.setNoDataTextColor(Color.YELLOW)

        // Leyenda
        val legend: Legend = lineChart.legend
        legend.isEnabled = true
        legend.textColor = ContextCompat.getColor(this, R.color.primary_text)
        legend.textSize = 11f

        // Eje X (tiempo)
        val xAxis: XAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = ContextCompat.getColor(this, R.color.secondary_text)
        xAxis.textSize = 10f
        xAxis.granularity = 1f

        // Eje Y izquierda
        val leftAxis = lineChart.axisLeft
        leftAxis.textColor = ContextCompat.getColor(this, R.color.primary_text)
        leftAxis.textSize = 10f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.DKGRAY
        leftAxis.setDrawAxisLine(false)

        // Eje Y derecha desactivado
        lineChart.axisRight.isEnabled = false
    }

    private fun loadDataAndShow(metric: ChartMetric, forceRefresh: Boolean) {
        tvChartStatus.visibility = View.VISIBLE
        tvChartStatus.text = "Cargando datos..."

        val cached = MeteoRepository.getCachedMeasurements(stationId)
        if (!cached.isNullOrEmpty() && !forceRefresh) {
            showChart(cached, metric)
        }

        MeteoRepository.refreshMeasurements(
            stationId,
            onSuccess = {
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        val data = MeteoRepository.getCachedMeasurements(stationId) ?: emptyList()
                        showChart(data, metric)
                    }
                }
            },
            onError = {
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        if (lineChart.data == null) {
                            lineChart.clear()
                            tvChartStatus.text = "Error al cargar datos"
                            tvChartStatus.visibility = View.VISIBLE
                        } else {
                            tvChartStatus.text = ""
                            tvChartStatus.visibility = View.GONE
                        }
                    }
                }
            }
        )
    }

    private fun showChart(
        measurements: List<MeteoMeasurement>,
        metric: ChartMetric
    ) {
        if (measurements.isEmpty()) {
            lineChart.clear()
            tvChartStatus.text = "No hay datos para mostrar"
            tvChartStatus.visibility = View.VISIBLE
            return
        }

        tvChartStatus.visibility = View.GONE

        val sorted = measurements.sortedBy { it.timestampMillis }
        val timestamps = sorted.map { it.timestampMillis }

        val entries = ArrayList<Entry>()
        val valuesForMean = ArrayList<Double>()

        sorted.forEachIndexed { index, m ->
            val value = when (metric) {
                ChartMetric.TEMPERATURE -> m.temperature
                ChartMetric.HUMIDITY -> m.humidity
            }
            entries.add(Entry(index.toFloat(), value.toFloat()))
            valuesForMean.add(value)
        }

        val label = when (metric) {
            ChartMetric.TEMPERATURE -> "Temperatura (°C)"
            ChartMetric.HUMIDITY -> "Humedad (%)"
        }

        val lineColor = ContextCompat.getColor(this, R.color.accent_green)

        val set = LineDataSet(entries, label).apply {
            color = lineColor
            lineWidth = 2.5f
            setDrawCircles(true)
            circleRadius = 3.5f
            setCircleColor(lineColor)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(false)
        }

        val data = LineData(set)
        lineChart.data = data

        // Media como línea horizontal
        val mean = valuesForMean.average().toFloat()
        val leftAxis = lineChart.axisLeft
        leftAxis.removeAllLimitLines()

        val meanLine = LimitLine(mean, "Media").apply {
            enableDashedLine(10f, 10f, 0f)
            lineWidth = 1.5f
            textSize = 10f
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP

            // Aquí NO usamos lineColor,
            // sino directamente:
            setLineColor(Color.parseColor("#FFA726"))
            setTextColor(Color.parseColor("#FFA726"))
        }


        leftAxis.addLimitLine(meanLine)

        // Rango Y con margen
        val minVal = valuesForMean.minOrNull() ?: mean.toDouble()
        val maxVal = valuesForMean.maxOrNull() ?: mean.toDouble()
        val padding = (maxVal - minVal).coerceAtLeast(1.0) * 0.15
        leftAxis.axisMinimum = (minVal - padding).toFloat()
        leftAxis.axisMaximum = (maxVal + padding).toFloat()

        // Eje X formateado como hora real
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val index = value.toInt().coerceIn(0, timestamps.size - 1)
                return timeFormatter.format(Date(timestamps[index]))
            }
        }

        // Marker para ver valor exacto al pulsar
        val marker = TimeValueMarker(
            context = this,
            layoutResId = R.layout.marker_view,
            timestamps = timestamps,
            metric = metric,
            fullFormatter = fullFormatter
        )
        lineChart.marker = marker   // <-- aquí solo asignamos el marker, nada de chartView

        lineChart.invalidate()
    }
}
