package com.teka.angaahewa.modules.land_measurement

import android.Manifest
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _currentPoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    val currentPoints: StateFlow<List<GpsPoint>> = _currentPoints.asStateFlow()
    
    private val _measurements = MutableStateFlow<List<Measurement>>(emptyList())
    val measurements: StateFlow<List<Measurement>> = _measurements.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<GpsPoint?>(null)
    val currentLocation: StateFlow<GpsPoint?> = _currentLocation.asStateFlow()
    
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var cancellationTokenSource = CancellationTokenSource()
    
    fun setLocationPermission(hasPermission: Boolean) {
        _hasLocationPermission.value = hasPermission
    }
    
    fun startTracking() {
        if (_hasLocationPermission.value) {
            _isTracking.value = true
            _currentPoints.value = emptyList()
            startLocationUpdates()
        }
    }
    
    fun stopTracking() {
        _isTracking.value = false
        cancellationTokenSource.cancel()
        cancellationTokenSource = CancellationTokenSource()
    }
    
    fun clearPoints() {
        _currentPoints.value = emptyList()
        _isTracking.value = false
        cancellationTokenSource.cancel()
        cancellationTokenSource = CancellationTokenSource()
    }
    
    fun saveCurrentMeasurement(): Boolean {
        val points = _currentPoints.value
        if (points.size >= 3) {
            val area = calculatePolygonArea(points)
            val perimeter = calculatePerimeter(points)
            val measurement = Measurement(
                id = "measurement_${System.currentTimeMillis()}",
                points = points,
                area = area,
                perimeter = perimeter,
                timestamp = System.currentTimeMillis()
            )
            _measurements.value = _measurements.value + measurement
            _currentPoints.value = emptyList()
            _isTracking.value = false
            return true
        }
        return false
    }
    
    fun deleteMeasurement(measurementId: String) {
        _measurements.value = _measurements.value.filter { it.id != measurementId }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    private fun startLocationUpdates() {
        viewModelScope.launch {
            while (_isTracking.value && _hasLocationPermission.value) {
                try {
                    val location = getCurrentLocation()
                    location?.let { loc ->
                        val gpsPoint = GpsPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy
                        )
                        _currentLocation.value = gpsPoint
                        _currentPoints.value = _currentPoints.value + gpsPoint
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error getting location: ${e.message}"
                }
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getCurrentLocation(): Location? {
        return try {
            val location = fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            location.result
        } catch (e: SecurityException) {
            _errorMessage.value = "Location permission denied"
            null
        } catch (e: Exception) {
            _errorMessage.value = "Failed to get location: ${e.message}"
            null
        }
    }
    
    // Calculate polygon area using Shoelace formula
    fun calculatePolygonArea(points: List<GpsPoint>): Double {
        if (points.size < 3) return 0.0
        
        // Convert to meters using UTM-like projection
        val projectedPoints = points.map { point ->
            val x = point.longitude * 111320.0 * cos(Math.toRadians(point.latitude))
            val y = point.latitude * 110540.0
            Pair(x, y)
        }
        
        var area = 0.0
        val n = projectedPoints.size
        
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += projectedPoints[i].first * projectedPoints[j].second
            area -= projectedPoints[j].first * projectedPoints[i].second
        }
        
        return abs(area) / 2.0
    }
    
    // Calculate perimeter
    fun calculatePerimeter(points: List<GpsPoint>): Double {
        if (points.size < 2) return 0.0
        
        var perimeter = 0.0
        for (i in 0 until points.size) {
            val current = points[i]
            val next = points[(i + 1) % points.size]
            perimeter += haversineDistance(current.latitude, current.longitude, next.latitude, next.longitude)
        }
        
        return perimeter
    }
    
    // Haversine distance formula
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    
    override fun onCleared() {
        super.onCleared()
        cancellationTokenSource.cancel()
    }
}