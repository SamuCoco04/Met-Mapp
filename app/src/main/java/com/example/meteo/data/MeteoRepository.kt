package com.example.meteo

import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt

data class MeteoMeasurement(
    val temperature: Double,
    val humidity: Double,
    val timestampMillis: Long
)

data class StatsSummary(
    val mean: Double?,
    val median: Double?,
    val mode: Double?,
    val stdDev: Double?
)

object MeteoRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Estación por defecto
    private const val DEFAULT_STATION_ID = "STATION_00"

    // Caché para compartir datos entre pantallas
    private val cache: MutableMap<String, List<MeteoMeasurement>> = mutableMapOf()

    fun getDefaultStationId(): String = DEFAULT_STATION_ID

    /**
     * Descarga todas las mediciones de una estación y las deja en caché.
     * Es MUY tolerante: si falta timestamp, inventa uno basado en el índice.
     */
    fun refreshMeasurements(
        stationId: String = DEFAULT_STATION_ID,
        onSuccess: (List<MeteoMeasurement>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(stationId)
            .get()
            .addOnSuccessListener { snapshot ->

                val measurements = snapshot.documents.mapIndexedNotNull { index, doc ->
                    // Temperatura: buscamos distintos posibles nombres
                    val temp = doc.getDouble("temperatura")
                        ?: doc.getDouble("temperature")
                        ?: doc.getDouble("temp")

                    // Humedad: distintos posibles nombres
                    val hum = doc.getDouble("humidade")
                        ?: doc.getDouble("humedad")
                        ?: doc.getDouble("humidade_relativa")
                        ?: doc.getDouble("humidity")

                    if (temp == null || hum == null) {
                        // Si faltan, ignoramos este documento
                        null
                    } else {
                        // Timestamp: probamos varios campos, y si no, inventamos uno
                        val ts = doc.getLong("timestampMillis")
                            ?: doc.getLong("timestamp")
                            ?: doc.id.toLongOrNull()
                            ?: index.toLong()  // al menos mantiene el orden

                        MeteoMeasurement(
                            temperature = temp,
                            humidity = hum,
                            timestampMillis = ts
                        )
                    }
                }

                cache[stationId] = measurements
                onSuccess(measurements)
            }
            .addOnFailureListener { ex ->
                onError(ex)
            }
    }

    fun getCachedMeasurements(stationId: String = DEFAULT_STATION_ID): List<MeteoMeasurement>? =
        cache[stationId]

    fun getLatestMeasurement(stationId: String = DEFAULT_STATION_ID): MeteoMeasurement? {
        val list = cache[stationId]
        if (list.isNullOrEmpty()) return null
        // Como hemos preservado el orden de los documentos, el último es el más reciente
        return list.maxByOrNull { it.timestampMillis }
    }

    fun computeStats(values: List<Double>): StatsSummary {
        if (values.isEmpty()) return StatsSummary(null, null, null, null)

        val sorted = values.sorted()
        val n = sorted.size

        val mean = sorted.sum() / n

        val median = if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }

        val frequency = sorted.groupingBy { it }.eachCount()
        val maxFreq = frequency.values.maxOrNull()
        val mode = frequency.entries.firstOrNull { it.value == maxFreq }?.key

        val variance = sorted.fold(0.0) { acc, value ->
            val diff = value - mean
            acc + diff * diff
        } / n
        val stdDev = sqrt(variance)

        return StatsSummary(mean, median, mode, stdDev)
    }
}
