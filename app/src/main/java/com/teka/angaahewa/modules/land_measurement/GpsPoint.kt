package com.teka.angaahewa.modules.land_measurement

data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class Measurement(
    val id: String,
    val points: List<GpsPoint>,
    val area: Double,
    val perimeter: Double,
    val timestamp: Long
)
