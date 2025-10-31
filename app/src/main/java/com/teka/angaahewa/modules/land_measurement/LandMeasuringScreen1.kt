package com.teka.angaahewa.modules.land_measurement

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


@Composable
fun LandMeasuringScreen(
    fusedLocationClient: FusedLocationProviderClient,
    cancellationTokenSource: CancellationTokenSource
) {
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isTracking by remember { mutableStateOf(false) }
    var currentPoints by remember { mutableStateOf<List<GpsPoint>>(emptyList()) }
    var measurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<GpsPoint?>(null) }
    
    val context = LocalContext.current
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        hasLocationPermission = fineLocationGranted || coarseLocationGranted
        
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Location tracking effect
    LaunchedEffect(isTracking, hasLocationPermission) {
        if (isTracking && hasLocationPermission) {
            while (isTracking) {
                try {
                    val location = getCurrentLocation(fusedLocationClient, cancellationTokenSource)
                    location?.let { loc ->
                        val gpsPoint = GpsPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy
                        )
                        currentLocation = gpsPoint
                        currentPoints = currentPoints + gpsPoint
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                delay(2000) // Update every 2 seconds
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Land Measuring App",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Current status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Current Status", fontWeight = FontWeight.Bold)
                Text("Points collected: ${currentPoints.size}")
                currentLocation?.let { loc ->
                    Text("Last location: ${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}")
                    Text("Accuracy: ±${loc.accuracy.roundToInt()}m")
                }
                
                if (currentPoints.size >= 3) {
                    val area = calculatePolygonArea(currentPoints)
                    val perimeter = calculatePerimeter(currentPoints)
                    Text("Current area: ${String.format("%.2f", area)} sq meters")
                    Text("Current perimeter: ${String.format("%.2f", perimeter)} meters")
                }
            }
        }

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (hasLocationPermission) {
                        isTracking = !isTracking
                        if (isTracking) {
                            currentPoints = emptyList()
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isTracking) "Stop Tracking" else "Start Tracking")
            }

            Button(
                onClick = {
                    if (currentPoints.size >= 3) {
                        val area = calculatePolygonArea(currentPoints)
                        val perimeter = calculatePerimeter(currentPoints)
                        val measurement = Measurement(
                            id = "measurement_${System.currentTimeMillis()}",
                            points = currentPoints,
                            area = area,
                            perimeter = perimeter,
                            timestamp = System.currentTimeMillis()
                        )
                        measurements = measurements + measurement
                        currentPoints = emptyList()
                        isTracking = false
                        Toast.makeText(context, "Measurement saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Need at least 3 points to save", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = currentPoints.size >= 3
            ) {
                Text("Save Area")
            }
        }

        Button(
            onClick = {
                currentPoints = emptyList()
                isTracking = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Clear Points")
        }

        // Saved measurements
        Text(
            text = "Saved Measurements (${measurements.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(measurements) { measurement ->
                MeasurementCard(
                    measurement = measurement,
                    onDelete = { measurements = measurements.filter { it.id != measurement.id } }
                )
            }
        }
    }
}

@Composable
fun MeasurementCard(measurement: Measurement, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Area: ${String.format("%.2f", measurement.area)} sq m",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Text("Perimeter: ${String.format("%.2f", measurement.perimeter)} m")
            Text("Points: ${measurement.points.size}")
            Text("Area (acres): ${String.format("%.4f", measurement.area * 0.000247105)}")
            Text("Area (hectares): ${String.format("%.4f", measurement.area * 0.0001)}")
            
            val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(Date(measurement.timestamp))
            Text("Measured: $date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
suspend fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    cancellationTokenSource: CancellationTokenSource
): Location? {
    return try {
        val location = fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
        location.result
    } catch (e: SecurityException) {
        null
    } catch (e: Exception) {
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
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
