package com.example.meteo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.meteo.data.MeteoRepository
import com.example.meteo.data.SensorReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTempAvg: TextView
    private lateinit var tvTempMin: TextView
    private lateinit var tvTempMax: TextView
    private lateinit var tvHumidityAvg: TextView
    private lateinit var tvHumidityMin: TextView
    private lateinit var tvHumidityMax: TextView
    private lateinit var temperatureChart: LineChart
    private lateinit var humidityChart: LineChart
    private lateinit var btnOpenStats: Button
    private val repository = MeteoRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a las vistas
        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        tvStatus = findViewById(R.id.tvStatus)
        tvTempAvg = findViewById(R.id.tvTempAvg)
        tvTempMin = findViewById(R.id.tvTempMin)
        tvTempMax = findViewById(R.id.tvTempMax)
        tvHumidityAvg = findViewById(R.id.tvHumidityAvg)
        tvHumidityMin = findViewById(R.id.tvHumidityMin)
        tvHumidityMax = findViewById(R.id.tvHumidityMax)
        temperatureChart = findViewById(R.id.tempChart)
        humidityChart = findViewById(R.id.humidityChart)
        btnOpenStats = findViewById(R.id.btnOpenStats)

        tvStatus.text = "Cargando últimos datos..."

        btnOpenStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        repository.fetchRecentReadings(
            onSuccess = { readings ->
                if (readings.isNotEmpty()) {
                    val latest = readings.first()

                    Log.d(
                        "Firestore",
                        "Último doc ${latest.id} -> temp=${latest.temperature} hum=${latest.humidity} ts=${latest.timestamp}"
                    )

                    tvTemperature.text = latest.temperature?.let { String.format(Locale.getDefault(), "%.1f ºC", it) } ?: "--"
                    tvHumidity.text = latest.humidity?.let { String.format(Locale.getDefault(), "%.1f %%", it) } ?: "--"
                    tvTimestamp.text = formatTimestamp(latest.timestamp)
                    tvStatus.text = "Último documento: ${latest.id}"

                    applyStatistics(readings)
                    renderCharts(readings)
                } else {
                    tvStatus.text = "No se encontraron datos"
                }
            },
            onError = { error ->
                Log.e("Firestore", "Erro ao ler dados", error)
                tvStatus.text = "Error al cargar datos"
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
