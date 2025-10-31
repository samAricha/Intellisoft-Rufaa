package com.teka.angaahewa.modules.farms_module

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.teka.angaahewa.utils.location.LocationState
import com.teka.angaahewa.utils.location.LocationViewModel
import com.teka.angaahewa.utils.location.useLocation
import com.teka.angaahewa.ui.theme.CarbonGray
import com.teka.angaahewa.ui.theme.ClimateGreen
import com.teka.angaahewa.ui.theme.EnergyBlue
import com.teka.angaahewa.ui.theme.SolarYellow
import com.teka.angaahewa.ui.theme.SuccessGreen
import com.teka.angaahewa.ui.theme.WarningOrange


@Composable
fun CreateFarmDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreateFarm: (String, Double, Double, String, String, Int, Double?) -> Unit,
    onLocationRequest: () -> Unit = {}
) {
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var acres by remember { mutableStateOf("") }
    var hectares by remember { mutableStateOf("") }

    var addressError by remember { mutableStateOf(false) }
    var acresError by remember { mutableStateOf(false) }

    // Get the location state from your location hook
    val locationState = useLocation()

    // Get the view model properly
    val locationViewModel: LocationViewModel = hiltViewModel()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, retry location fetch
            locationViewModel.retryLocationFetch()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isCreating,
            dismissOnClickOutside = !isCreating
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ClimateGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ClimateGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Add Carbon Site",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray
                    )
                }

                Text(
                    text = "Create a new carbon monitoring site to track your environmental impact",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )

                // Location Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClimateGreen.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ClimateGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Location Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = CarbonGray
                            )
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                addressError = false
                            },
                            label = { Text("Address") },
                            placeholder = { Text("Enter site address") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = addressError,
                            supportingText = if (addressError) {
                                { Text("Address is required", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                            }
                        )

                        // Auto-detected Location Display Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when (locationState) {
                                    is LocationState.Success -> SuccessGreen.copy(alpha = 0.1f)
                                    is LocationState.Error -> Color(0xFFE57373).copy(alpha = 0.1f)
                                    is LocationState.PermissionDenied -> WarningOrange.copy(alpha = 0.1f)
                                    else -> Color(0xFFF5F5F5)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (locationState) {
                                    is LocationState.Loading -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = ClimateGreen,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Auto-detecting location...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF666666)
                                        )
                                    }

                                    is LocationState.Success -> {
                                        val location = locationState.location
                                        Icon(
                                            Icons.Default.GpsFixed,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Location Auto-Detected",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                        Text(
                                            text = "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF666666)
                                        )
                                        location.accuracy?.let { accuracy ->
                                            Text(
                                                text = "Accuracy: ${String.format("%.0f", accuracy)}m",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF888888)
                                            )
                                        }
                                        location.address?.let { detectedAddress ->
                                            if (address.isBlank()) {
                                                // Auto-fill address field with detected address
                                                LaunchedEffect(detectedAddress) {
                                                    address = detectedAddress
                                                }
                                            }
                                            Text(
                                                text = "Detected: $detectedAddress",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF666666),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    is LocationState.Error -> {
                                        Icon(
                                            Icons.Default.LocationOff,
                                            contentDescription = null,
                                            tint = Color(0xFFE57373),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Location Detection Failed",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE57373)
                                        )
                                        Text(
                                            text = locationState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF666666),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "You can still create the site by entering coordinates manually",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF888888),
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { locationViewModel.retryLocationFetch() },
                                            colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry Location")
                                        }
                                    }

                                    is LocationState.PermissionDenied -> {
                                        Icon(
                                            Icons.Default.LocationDisabled,
                                            contentDescription = null,
                                            tint = WarningOrange,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Location Permission Required",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = WarningOrange
                                        )
                                        Text(
                                            text = "Grant location permission to auto-detect site coordinates",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF666666),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Or enter coordinates manually below",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF888888),
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Grant Permission")
                                        }
                                    }
                                }
                            }
                        }

                        // Manual coordinate input (only show when location detection fails)
                        if (locationState is LocationState.Error || locationState is LocationState.PermissionDenied) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Manual Coordinates (Optional)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666),
                                    fontWeight = FontWeight.Medium
                                )

                                var latitude by remember { mutableStateOf("") }
                                var longitude by remember { mutableStateOf("") }
                                var latError by remember { mutableStateOf(false) }
                                var lngError by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = latitude,
                                        onValueChange = {
                                            latitude = it
                                            latError = false
                                        },
                                        label = { Text("Latitude") },
                                        placeholder = { Text("-1.286389") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        isError = latError,
                                        supportingText = if (latError) {
                                            { Text("Invalid", color = MaterialTheme.colorScheme.error) }
                                        } else null
                                    )
                                    OutlinedTextField(
                                        value = longitude,
                                        onValueChange = {
                                            longitude = it
                                            lngError = false
                                        },
                                        label = { Text("Longitude") },
                                        placeholder = { Text("36.817223") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        isError = lngError,
                                        supportingText = if (lngError) {
                                            { Text("Invalid", color = MaterialTheme.colorScheme.error) }
                                        } else null
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                placeholder = { Text("Optional") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State/County") },
                                placeholder = { Text("Optional") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Area Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = EnergyBlue.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Landscape,
                                contentDescription = null,
                                tint = EnergyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Area Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = CarbonGray
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = acres,
                                onValueChange = {
                                    acres = it
                                    acresError = false
                                },
                                label = { Text("Acres") },
                                placeholder = { Text("0") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = acresError,
                                supportingText = if (acresError) {
                                    { Text("Required", color = MaterialTheme.colorScheme.error) }
                                } else null,
                                leadingIcon = {
                                    Icon(Icons.Default.Landscape, contentDescription = null)
                                }
                            )
                            OutlinedTextField(
                                value = hectares,
                                onValueChange = { hectares = it },
                                label = { Text("Hectares") },
                                placeholder = { Text("Optional") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        // Carbon impact preview
                        val acresInt = acres.toIntOrNull() ?: 0
                        if (acresInt > 0) {
                            val estimatedCarbon = acresInt * 2.3
                            val estimatedValue = estimatedCarbon * 15.0

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SolarYellow.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Eco,
                                        contentDescription = null,
                                        tint = SolarYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Est. ${String.format("%.1f", estimatedCarbon)}t CO₂ • KSH${String.format("%.0f", estimatedValue)} value",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CarbonGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isCreating
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            // Get coordinates from location state or manual input
                            val (finalLat, finalLng) = when (locationState) {
                                is LocationState.Success -> {
                                    Pair(locationState.location.latitude, locationState.location.longitude)
                                }
                                else -> {
                                    // Use manual input if location detection failed
                                    val manualLat = /* get from manual input state */ 0.0 // You'll need to manage these states
                                    val manualLng = /* get from manual input state */ 0.0
                                    Pair(manualLat, manualLng)
                                }
                            }

                            // Validation
                            addressError = address.isBlank()
                            acresError = acres.toIntOrNull() == null || acres.toIntOrNull()!! <= 0

                            if (!addressError && !acresError) {
                                onCreateFarm(
                                    address,
                                    finalLat,
                                    finalLng,
                                    city,
                                    state,
                                    acres.toInt(),
                                    hectares.toDoubleOrNull()
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen),
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create Site")
                        }
                    }
                }

                // Helper text
                if (locationState !is LocationState.Success) {
                    Text(
                        text = "Location auto-detection is optional but recommended for accurate site mapping",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}




@Composable
fun UpdateFarmDialog(
    farm: FarmResponse,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onUpdateFarm: (String, String, Double, Double, String, String, Int, Double?) -> Unit
) {
    var address by remember { mutableStateOf(farm.location?.address ?: "") }
    var latitude by remember { mutableStateOf(farm.location?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(farm.location?.longitude?.toString() ?: "") }
    var city by remember { mutableStateOf(farm.location?.city ?: "") }
    var state by remember { mutableStateOf(farm.location?.state ?: "") }
    var acres by remember { mutableStateOf(farm.location?.acres?.toString() ?: "") }
    var hectares by remember { mutableStateOf(farm.location?.area_hectares?.toString() ?: "") }

    var addressError by remember { mutableStateOf(false) }
    var latError by remember { mutableStateOf(false) }
    var lngError by remember { mutableStateOf(false) }
    var acresError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isUpdating,
            dismissOnClickOutside = !isUpdating
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EnergyBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = EnergyBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Edit Carbon Site",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                        Text(
                            text = farm.profiles?.name ?: "Site",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }

                // Location Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClimateGreen.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ClimateGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Location Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = CarbonGray
                            )
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                addressError = false
                            },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = addressError,
                            supportingText = if (addressError) {
                                { Text("Address is required", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = latitude,
                                onValueChange = {
                                    latitude = it
                                    latError = false
                                },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = latError,
                                supportingText = if (latError) {
                                    { Text("Invalid", color = MaterialTheme.colorScheme.error) }
                                } else null
                            )
                            OutlinedTextField(
                                value = longitude,
                                onValueChange = {
                                    longitude = it
                                    lngError = false
                                },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = lngError,
                                supportingText = if (lngError) {
                                    { Text("Invalid", color = MaterialTheme.colorScheme.error) }
                                } else null
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State/County") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Area Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = EnergyBlue.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Landscape,
                                contentDescription = null,
                                tint = EnergyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Area Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = CarbonGray
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = acres,
                                onValueChange = {
                                    acres = it
                                    acresError = false
                                },
                                label = { Text("Acres") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = acresError,
                                supportingText = if (acresError) {
                                    { Text("Required", color = MaterialTheme.colorScheme.error) }
                                } else null,
                                leadingIcon = {
                                    Icon(Icons.Default.Landscape, contentDescription = null)
                                }
                            )
                            OutlinedTextField(
                                value = hectares,
                                onValueChange = { hectares = it },
                                label = { Text("Hectares") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            // Validation
                            addressError = address.isBlank()
                            latError = latitude.toDoubleOrNull() == null
                            lngError = longitude.toDoubleOrNull() == null
                            acresError = acres.toIntOrNull() == null || acres.toIntOrNull()!! <= 0

                            if (!addressError && !latError && !lngError && !acresError) {
                                onUpdateFarm(
                                    farm.id,
                                    address,
                                    latitude.toDouble(),
                                    longitude.toDouble(),
                                    city,
                                    state,
                                    acres.toInt(),
                                    hectares.toDoubleOrNull()
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyBlue),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update Site")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteFarmDialog(
    farm: FarmResponse,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onDeleteFarm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Delete Carbon Site",
                    style = MaterialTheme.typography.titleLarge,
                    color = CarbonGray
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete this carbon monitoring site?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CarbonGray
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = farm.profiles?.name ?: "Carbon Site",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                        if (farm.location?.address != null) {
                            Text(
                                text = farm.location.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                        val acres = farm.location?.acres ?: 0
                        if (acres > 0) {
                            val carbonCredits = acres * 2.3
                            Text(
                                text = "$acres acres • ${String.format("%.1f", carbonCredits)}t CO₂ potential",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarYellow,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = "This action cannot be undone. All monitoring data and carbon credit calculations for this site will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE57373),
                    textAlign = TextAlign.Start
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDeleteFarm(farm.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel", color = CarbonGray)
            }
        }
    )
}