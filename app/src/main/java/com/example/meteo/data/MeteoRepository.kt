package com.example.meteo.data

import com.example.meteo.model.Measurement
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MeteoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getLatestMeasurementForStation(
        stationCollection: String,
        onResult: (Measurement?) -> Unit
    ) {
        db.collection(stationCollection)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                // Helper genérico para pasar cualquier número / string a Double
                fun asDouble(value: Any?): Double? = when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull()
                    else -> null
                }

                // --- TIMESTAMP ---
                val rawTs = doc.get("timestamp")
                val timestampMillis: Long? = when (rawTs) {
                    is Timestamp -> rawTs.toDate().time
                    is Number -> {
                        val v = rawTs.toLong()
                        // Si es más pequeño que 10^12 asumimos que son SEGUNDOS → pasamos a milis
                        if (v < 1_000_000_000_000L) v * 1000 else v
                    }
                    is String -> rawTs.toLongOrNull()?.let { v ->
                        if (v < 1_000_000_000_000L) v * 1000 else v
                    }
                    else -> null
                }

                // --- TEMPERATURA ---
                val temp = asDouble(
                    doc.get("temperature")
                        ?: doc.get("temperatura")
                        ?: doc.get("temp")
                )

                // --- HUMEDAD ---
                val hum = asDouble(
                    doc.get("humidity")
                        ?: doc.get("humedad")
                        ?: doc.get("hum")
                        ?: doc.get("umidade")   // pt-BR
                        ?: doc.get("humidade")  // pt-PT
                        ?: doc.get("RH")
                )

                val measurement = Measurement(
                    timestampMillis = timestampMillis,
                    temperature = temp,
                    humidity = hum
                )

                onResult(measurement)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
