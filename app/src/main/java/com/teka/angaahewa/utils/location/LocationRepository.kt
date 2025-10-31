package com.teka.angaahewa.utils.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationRepository"
        private const val LOCATION_TIMEOUT_MS = 10_000L // 10 seconds
    }

    suspend fun getCurrentLocation(): LocationState = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting location fetch...")

            if (!hasLocationPermission()) {
                Log.d(TAG, "No location permission")
                return@withContext LocationState.PermissionDenied
            }

            if (!isLocationEnabled()) {
                Log.d(TAG, "Location services disabled")
                return@withContext LocationState.Error("Location services are disabled. Please enable GPS in your device settings.")
            }

            Log.d(TAG, "Permissions OK, fetching location with Fused Location Client...")

            // Try FusedLocationClient first
            val fusedLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                suspendCoroutine<Location?> { continuation ->
                    val cancellationToken = CancellationTokenSource()

                    try {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY, // Try balanced instead of high accuracy
                            cancellationToken.token
                        ).addOnSuccessListener { location ->
                            Log.d(TAG, "Fused location success: $location")
                            continuation.resume(location)
                        }.addOnFailureListener { exception ->
                            Log.e(TAG, "Fused location failed", exception)
                            continuation.resume(null) // Don't throw, try fallback
                        }.addOnCanceledListener {
                            Log.d(TAG, "Fused location request canceled")
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception with fused location client", e)
                        continuation.resume(null)
                    }
                }
            }

            // If fused location worked, return it
            if (fusedLocation != null) {
                Log.d(TAG, "Got fused location: ${fusedLocation.latitude}, ${fusedLocation.longitude}")
                return@withContext LocationState.Success(
                    LocationData(
                        latitude = fusedLocation.latitude,
                        longitude = fusedLocation.longitude,
                        accuracy = fusedLocation.accuracy,
                        timestamp = fusedLocation.time
                    )
                )
            }

            // Fallback to last known location
            Log.d(TAG, "Fused location failed, trying last known location...")
            val lastLocation = withTimeoutOrNull(5000L) {
                suspendCoroutine<Location?> { continuation ->
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            Log.d(TAG, "Last known location: $location")
                            continuation.resume(location)
                        }.addOnFailureListener { exception ->
                            Log.e(TAG, "Last location failed", exception)
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception getting last location", e)
                        continuation.resume(null)
                    }
                }
            }

            if (lastLocation != null) {
                Log.d(TAG, "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
                return@withContext LocationState.Success(
                    LocationData(
                        latitude = lastLocation.latitude,
                        longitude = lastLocation.longitude,
                        accuracy = lastLocation.accuracy,
                        timestamp = lastLocation.time
                    )
                )
            }

            Log.d(TAG, "All location methods failed")
            LocationState.Error("Unable to get current location. Please ensure GPS is enabled and you're in an area with good signal, then try again.")

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception", e)
            LocationState.PermissionDenied
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting location", e)
            LocationState.Error("Failed to get location: ${e.message}", e)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
}