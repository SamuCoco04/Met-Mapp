package com.example.meteo.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MeteoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun fetchRecentReadings(
        limit: Long = 50,
        onSuccess: (List<SensorReading>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("STATION_01")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { result ->
                val readings = result.documents.map { document ->
                    SensorReading(
                        id = document.id,
                        temperature = document.getDouble("temperatura"),
                        humidity = document.getDouble("humidade"),
                        timestamp = document.getLong("timestamp")
                    )
                }
                onSuccess(readings)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}
