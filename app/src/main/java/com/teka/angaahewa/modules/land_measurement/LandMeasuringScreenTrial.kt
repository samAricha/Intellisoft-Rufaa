package com.teka.angaahewa.modules.land_measurement

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LandMeasuringScreenTrial(
    navController: NavHostController,
    viewModel: LocationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Collect state from ViewModel
    val isTracking by viewModel.isTracking.collectAsState()
    val currentPoints by viewModel.currentPoints.collectAsState()
    val measurements by viewModel.measurements.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.setLocationPermission(hasPermission)
    }

    // Check permissions on first launch
    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasPermission = fineLocationGranted || coarseLocationGranted
        viewModel.setLocationPermission(hasPermission)
        
        if (!hasPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
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
                    val area = viewModel.calculatePolygonArea(currentPoints)
                    val perimeter = viewModel.calculatePerimeter(currentPoints)
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
                        if (isTracking) {
                            viewModel.stopTracking()
                        } else {
                            viewModel.startTracking()
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
                    val saved = viewModel.saveCurrentMeasurement()
                    if (saved) {
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
            onClick = { viewModel.clearPoints() },
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
                MeasurementCard1(
                    measurement = measurement,
                    onDelete = { viewModel.deleteMeasurement(measurement.id) }
                )
            }
        }
    }
}

@Composable
fun MeasurementCard1(measurement: Measurement, onDelete: () -> Unit) {
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

