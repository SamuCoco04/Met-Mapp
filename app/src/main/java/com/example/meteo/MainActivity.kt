package com.example.meteo

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var tvStatus: TextView

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

        tvStatus.text = "Loading latest data..."

        val db = FirebaseFirestore.getInstance()

        db.collection("STATION_01")
            .orderBy("timestamp", Query.Direction.DESCENDING) // último dato
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]

                    val idTimestamp = document.id      // nombre del documento
                    val temperature = document.getDouble("temperatura")
                    val humidity = document.getDouble("humidade")
                    val timestamp = document.getLong("timestamp")

                    Log.d(
                        "Firestore",
                        "Doc: $idTimestamp -> temp=$temperature hum=$humidity ts=$timestamp"
                    )

                    tvTemperature.text = temperature?.let { "$it ºC" } ?: "--"
                    tvHumidity.text = humidity?.let { "$it % RH" } ?: "--"
                    tvTimestamp.text = timestamp?.toString() ?: "--"
                    tvStatus.text = "Last update: $idTimestamp"
                } else {
                    tvStatus.text = "No data found"
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao ler dados", e)
                tvStatus.text = "Error loading data"
            }
    }
}
