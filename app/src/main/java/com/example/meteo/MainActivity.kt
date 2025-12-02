package com.example.meteo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.meteo.data.MeteoRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val repository = MeteoRepository()

    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvTimestamp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // OJO: ajusta estos IDs a los que tengas en activity_main.xml
        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvTimestamp = findViewById(R.id.tvTimestamp)

        // De momento leemos solo la estación STATION_00
        loadLatestMeasurement("STATION_00")
    }

    private fun loadLatestMeasurement(stationId: String) {
        repository.getLatestMeasurementForStation(stationId) { latest ->
            if (latest == null) {
                tvTemperature.text = "-- °C"
                tvHumidity.text = "-- %"
                tvTimestamp.text = "No data"
                return@getLatestMeasurementForStation
            }

            val tempText = latest.temperature?.let { String.format("%.1f °C", it) } ?: "-- °C"
            val humText  = latest.humidity?.let { String.format("%.0f %%", it) } ?: "-- %"

            val dateText = latest.timestampMillis?.let { millis ->
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(millis))
            } ?: "--"

            tvTemperature.text = tempText
            tvHumidity.text    = humText
            tvTimestamp.text   = dateText
        }
    }
}
