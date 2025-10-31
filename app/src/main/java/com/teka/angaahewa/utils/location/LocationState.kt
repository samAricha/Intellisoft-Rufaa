package com.teka.angaahewa.utils.location

sealed class LocationState {
    object Loading : LocationState()
    object PermissionDenied : LocationState()
    data class Success(val location: LocationData) : LocationState()
    data class Error(val message: String, val exception: Throwable? = null) : LocationState()
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val address: String? = null
)