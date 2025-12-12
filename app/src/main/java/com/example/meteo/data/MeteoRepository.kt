package com.example.meteo

import com.google.firebase.firestore.FirebaseFirestore

object MeteoRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    data class Measurement(
        val temperature: Double,
        val humidity: Double,
        val particles: Double,
        val timestampMillis: Long
    )

    private val cache: MutableMap<String, List<Measurement>> = mutableMapOf()

    // Lee la lista de estaciones desde metadados/collections.name
    fun getStationIds(
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("metadados")
            .document("collections")
            .get()
            .addOnSuccessListener { doc ->
                val raw = doc.get("name")
                val ids = when (raw) {
                    is List<*> -> raw.filterIsInstance<String>()
                    is Array<*> -> raw.filterIsInstance<String>()
                    else -> emptyList()
                }
                onSuccess(ids)
            }
            .addOnFailureListener { ex ->
                onError(ex)
            }
    }

    // Actualiza lecturas de una estación
    fun refreshMeasurements(
        stationId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(stationId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { d ->
                    val temp = d.getDouble("temperatura")
                    val hum =
                        d.getDouble("humidade") ?: d.getDouble("humedad") ?: d.getDouble("humidade")
                    val part =
                        d.getDouble("particulas") ?: d.getDouble("partículas") ?: d.getDouble("particulas")
                    val tsSeconds = d.getLong("timestamp") ?: d.id.toLongOrNull()

                    if (temp == null || hum == null || part == null || tsSeconds == null) {
                        null
                    } else {
                        val tsMillis = tsSeconds * 1000L
                        Measurement(
                            temperature = temp,
                            humidity = hum,
                            particles = part,
                            timestampMillis = tsMillis
                        )
                    }
                }.sortedBy { it.timestampMillis }

                cache[stationId] = list
                onSuccess()
            }
            .addOnFailureListener { ex ->
                onError(ex)
            }
    }

    fun getCachedMeasurements(stationId: String): List<Measurement>? = cache[stationId]

    fun getLatestMeasurement(stationId: String): Measurement? =
        cache[stationId]?.maxByOrNull { it.timestampMillis }
}
