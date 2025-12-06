package com.example.meteo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnStatistics: Button

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val stationId = MeteoRepository.getDefaultStationId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        tvStatus = findViewById(R.id.tvStatus)
        btnStatistics = findViewById(R.id.btnStatistics)

        btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        loadLatestData()
    }

    private fun loadLatestData() {
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "Cargando datos..."

        val cached = MeteoRepository.getCachedMeasurements(stationId)
        if (cached != null && cached.isNotEmpty()) {
            updateCardWithLatest()
        }

        MeteoRepository.refreshMeasurements(
            stationId,
            onSuccess = {
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        tvStatus.visibility = View.GONE
                        updateCardWithLatest()
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

    private fun updateCardWithLatest() {
        val latest = MeteoRepository.getLatestMeasurement(stationId)
        if (latest == null) {
            tvTemperature.text = "-- °C"
            tvHumidity.text = "-- %"
            tvTimestamp.text = "--"
            return
        }

        tvTemperature.text = String.format(Locale.getDefault(), "%.1f °C", latest.temperature)
        tvHumidity.text = String.format(Locale.getDefault(), "%.1f %%", latest.humidity)
        tvTimestamp.text = dateFormatter.format(Date(latest.timestampMillis))
    }
}
