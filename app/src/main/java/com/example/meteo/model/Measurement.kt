package com.example.meteo.model

data class Measurement(
    // timestamp en milisegundos desde epoch (o null si no viene bien)
    val timestampMillis: Long? = null,
    val temperature: Double? = null,
    val humidity: Double? = null
)
