package com.example.meteo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.meteo.data.MeteoRepository
import com.example.meteo.data.SensorReading
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var tvTempAvg: TextView
    private lateinit var tvTempMin: TextView
    private lateinit var tvTempMax: TextView
    private lateinit var tvHumidityAvg: TextView
    private lateinit var tvHumidityMin: TextView
    private lateinit var tvHumidityMax: TextView
    private lateinit var tvLastTimestamp: TextView
    private lateinit var statusText: TextView
    private lateinit var temperatureChart: LineChart
    private lateinit var humidityChart: LineChart
    private lateinit var backButton: Button
    private val repository = MeteoRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.statistics_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvTempAvg = findViewById(R.id.tvTempAvgStats)
        tvTempMin = findViewById(R.id.tvTempMinStats)
        tvTempMax = findViewById(R.id.tvTempMaxStats)
        tvHumidityAvg = findViewById(R.id.tvHumidityAvgStats)
        tvHumidityMin = findViewById(R.id.tvHumidityMinStats)
        tvHumidityMax = findViewById(R.id.tvHumidityMaxStats)
        tvLastTimestamp = findViewById(R.id.tvLastTimestamp)
        temperatureChart = findViewById(R.id.tempChartStats)
        humidityChart = findViewById(R.id.humidityChartStats)
        statusText = findViewById(R.id.tvStatusStats)
        backButton = findViewById(R.id.btnBack)

        backButton.setOnClickListener { finish() }

        statusText.text = "Cargando historial..."
        fetchData()
    }

    private fun fetchData() {
        repository.fetchRecentReadings(
            onSuccess = { readings ->
                if (readings.isEmpty()) {
                    statusText.text = "No se encontraron datos"
                    return@fetchRecentReadings
                }

                val lastTimestamp = readings.firstOrNull()?.timestamp
                tvLastTimestamp.text = formatTimestamp(lastTimestamp)
                statusText.text = "Historial actualizado: ${readings.size} registros"

                applyStatistics(readings)
                renderCharts(readings)
            },
            onError = { error ->
                Log.e("Firestore", "Error al cargar historial", error)
                statusText.text = "Error al cargar datos"
            }
        )
    }

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null) return "--"
        return try {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            Log.w("Firestore", "No se pudo formatear la fecha", e)
            timestamp.toString()
        }
    }

    private fun applyStatistics(readings: List<SensorReading>) {
        val temps = readings.mapNotNull { it.temperature }
        val hums = readings.mapNotNull { it.humidity }

        val tempAvg = temps.takeIf { it.isNotEmpty() }?.average()
        tvTempAvg.text = tempAvg?.let { String.format(Locale.getDefault(), "%.1f ºC", it) } ?: "--"
        tvTempMin.text = temps.minOrNull()?.let { String.format(Locale.getDefault(), "%.1f ºC", it) } ?: "--"
        tvTempMax.text = temps.maxOrNull()?.let { String.format(Locale.getDefault(), "%.1f ºC", it) } ?: "--"

        val humidityAvg = hums.takeIf { it.isNotEmpty() }?.average()
        tvHumidityAvg.text = humidityAvg?.let { String.format(Locale.getDefault(), "%.1f %%", it) } ?: "--"
        tvHumidityMin.text = hums.minOrNull()?.let { String.format(Locale.getDefault(), "%.1f %%", it) } ?: "--"
        tvHumidityMax.text = hums.maxOrNull()?.let { String.format(Locale.getDefault(), "%.1f %%", it) } ?: "--"
    }

    private fun renderCharts(readings: List<SensorReading>) {
        val ordered = readings.sortedBy { it.timestamp ?: 0L }

        val tempEntries = ordered.mapIndexedNotNull { index, reading ->
            reading.temperature?.let { Entry(index.toFloat(), it.toFloat()) }
        }
        val humidityEntries = ordered.mapIndexedNotNull { index, reading ->
            reading.humidity?.let { Entry(index.toFloat(), it.toFloat()) }
        }

        setupChart(temperatureChart, tempEntries, "Temperatura (ºC)", 18f)
        setupChart(humidityChart, humidityEntries, "Humedad (%)", 28f)
    }

    private fun setupChart(chart: LineChart, entries: List<Entry>, label: String, startSpace: Float) {
        val dataSet = LineDataSet(entries, label).apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = getColor(R.color.primary_blue)
            setCircleColor(getColor(R.color.primary_blue))
            lineWidth = 2.5f
            circleRadius = 3.5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillColor = getColor(R.color.primary_blue)
            setDrawFilled(true)
            fillAlpha = 45
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setNoDataText("Sin datos para graficar")
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = getColor(R.color.primary_text)
                setDrawGridLines(false)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = getColor(R.color.primary_text)
                setDrawGridLines(false)
                spaceMin = startSpace
                spaceMax = 4f
                setAvoidFirstLastClipping(true)
            }
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            invalidate()
        }
    }
}
