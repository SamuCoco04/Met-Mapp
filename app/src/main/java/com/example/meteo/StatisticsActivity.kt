package com.example.meteo

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private enum class Metric { TEMPERATURE, HUMIDITY }

    private lateinit var tvBack: TextView
    private lateinit var tvStatsTitle: TextView
    private lateinit var tvStatus: TextView

    private lateinit var btnMetricTemp: Button
    private lateinit var btnMetricHum: Button

    private lateinit var tvMeanValue: TextView
    private lateinit var tvMedianValue: TextView
    private lateinit var tvModeValue: TextView
    private lateinit var tvStdDevValue: TextView

    private var currentMetric: Metric = Metric.TEMPERATURE
    private val stationId = MeteoRepository.getDefaultStationId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        tvBack = findViewById(R.id.tvBack)
        tvStatsTitle = findViewById(R.id.tvStatsTitle)
        tvStatus = findViewById(R.id.tvStatusStats)

        btnMetricTemp = findViewById(R.id.btnMetricTemp)
        btnMetricHum = findViewById(R.id.btnMetricHum)

        tvMeanValue = findViewById(R.id.tvMeanValue)
        tvMedianValue = findViewById(R.id.tvMedianValue)
        tvModeValue = findViewById(R.id.tvModeValue)
        tvStdDevValue = findViewById(R.id.tvStdDevValue)

        tvBack.setOnClickListener { finish() }

        btnMetricTemp.setOnClickListener {
            currentMetric = Metric.TEMPERATURE
            updateMetricButtons()
            updateStatsFromCache()
        }

        btnMetricHum.setOnClickListener {
            currentMetric = Metric.HUMIDITY
            updateMetricButtons()
            updateStatsFromCache()
        }

        updateMetricButtons()
        loadData()
    }

    private fun updateMetricButtons() {
        val selectedColor = resources.getColor(R.color.accent_green_soft, theme)
        val unselectedColor = resources.getColor(R.color.dark_card, theme)

        btnMetricTemp.setBackgroundColor(
            if (currentMetric == Metric.TEMPERATURE) selectedColor else unselectedColor
        )
        btnMetricHum.setBackgroundColor(
            if (currentMetric == Metric.HUMIDITY) selectedColor else unselectedColor
        )
    }

    private fun loadData() {
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "Cargando datos..."

        val cached = MeteoRepository.getCachedMeasurements(stationId)
        if (cached != null && cached.isNotEmpty()) {
            tvStatus.visibility = View.GONE
            updateStatsFromCache()
            return
        }

        MeteoRepository.refreshMeasurements(
            stationId,
            onSuccess = {
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        tvStatus.visibility = View.GONE
                        updateStatsFromCache()
                    }
                }
            },
            onError = { ex ->
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        tvStatus.visibility = View.VISIBLE
                        tvStatus.text = "Error al cargar datos"
                        Toast.makeText(
                            this,
                            "Error al obtener datos: ${ex.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun updateStatsFromCache() {
        val measurements = MeteoRepository.getCachedMeasurements(stationId)
        if (measurements == null || measurements.isEmpty()) {
            tvMeanValue.text = "--"
            tvMedianValue.text = "--"
            tvModeValue.text = "--"
            tvStdDevValue.text = "--"
            return
        }

        val values = when (currentMetric) {
            Metric.TEMPERATURE -> measurements.map { it.temperature }
            Metric.HUMIDITY -> measurements.map { it.humidity }
        }

        val stats = MeteoRepository.computeStats(values)

        val unit = if (currentMetric == Metric.TEMPERATURE) "°C" else "%"

        tvStatsTitle.text = if (currentMetric == Metric.TEMPERATURE) {
            "Estación Meteo - Temperatura"
        } else {
            "Estación Meteo - Humedad"
        }

        fun format(v: Double?): String =
            if (v == null) "--" else String.format(Locale.getDefault(), "%.2f %s", v, unit)

        tvMeanValue.text = format(stats.mean)
        tvMedianValue.text = format(stats.median)
        tvModeValue.text = format(stats.mode)
        tvStdDevValue.text = format(stats.stdDev)
    }
}
