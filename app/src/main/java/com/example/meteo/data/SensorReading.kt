package com.example.meteo.data

data class SensorReading(
    val id: String,
    val temperature: Double?,
    val humidity: Double?,
    val timestamp: Long?
)
