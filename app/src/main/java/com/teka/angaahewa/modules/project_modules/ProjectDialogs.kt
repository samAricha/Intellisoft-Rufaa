package com.teka.angaahewa.modules.project_modules


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
import androidx.hilt.navigation.compose.hiltViewModel
import com.teka.angaahewa.modules.home_module.ProjectResponse
import com.teka.angaahewa.utils.location.LocationState
import com.teka.angaahewa.utils.location.LocationViewModel
import com.teka.angaahewa.utils.location.useLocation

import com.teka.angaahewa.ui.theme.CarbonGray
import com.teka.angaahewa.ui.theme.ClimateGreen
import com.teka.angaahewa.ui.theme.EnergyBlue
import com.teka.angaahewa.ui.theme.SuccessGreen
import com.teka.angaahewa.ui.theme.WarningOrange



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreateProject: (String, String, String, Double, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("active") }
    var cropType by remember { mutableStateOf("") }
    var expectedYield by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

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

    val statusOptions = listOf(
        "active" to "Active",
        "inactive" to "Inactive",
        "completed" to "Completed"
    )

    Dialog(onDismissRequest = { if (!isCreating) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = ClimateGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "Create New Project",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isCreating
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Project Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("Enter project name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    leadingIcon = {
                        Icon(Icons.Default.Assignment, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClimateGreen,
                        focusedLabelColor = ClimateGreen
                    )
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe your project") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    minLines = 2,
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClimateGreen,
                        focusedLabelColor = ClimateGreen
                    )
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isCreating) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = statusOptions.find { it.first == status }?.second ?: "Active",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Status") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isCreating,
                        leadingIcon = {
                            Icon(
                                when (status) {
                                    "active" -> Icons.Default.PlayCircle
                                    "completed" -> Icons.Default.CheckCircle
                                    else -> Icons.Default.PauseCircle
                                },
                                contentDescription = null,
                                tint = when (status) {
                                    "active" -> SuccessGreen
                                    "completed" -> EnergyBlue
                                    else -> WarningOrange
                                }
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClimateGreen,
                            focusedLabelColor = ClimateGreen
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    status = value
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        when (value) {
                                            "active" -> Icons.Default.PlayCircle
                                            "completed" -> Icons.Default.CheckCircle
                                            else -> Icons.Default.PauseCircle
                                        },
                                        contentDescription = null,
                                        tint = when (value) {
                                            "active" -> SuccessGreen
                                            "completed" -> EnergyBlue
                                            else -> WarningOrange
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // Location Section Header
                Text(
                    text = "Project Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray
                )

                // Location Status Card
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
                                    text = "Getting your location...",
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
                                    text = "location auto-detected",
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
                                location.address?.let { address ->
                                    Text(
                                        text = address,
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
                                    text = "Location Error",
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
                                    Text("Retry")
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
                                    text = "Grant location permission to auto-detect project location",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666),
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

                // Crop Type
                OutlinedTextField(
                    value = cropType,
                    onValueChange = { cropType = it },
                    label = { Text("Crop Type (Optional)") },
                    placeholder = { Text("e.g., maize, wheat, vegetables") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    leadingIcon = {
                        Icon(Icons.Default.Grass, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClimateGreen,
                        focusedLabelColor = ClimateGreen
                    )
                )

                // Expected Yield
                OutlinedTextField(
                    value = expectedYield,
                    onValueChange = { expectedYield = it },
                    label = { Text("Expected Yield (Optional)") },
                    placeholder = { Text("e.g., 5000kg, 2 tons") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                    leadingIcon = {
                        Icon(Icons.Default.Agriculture, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClimateGreen,
                        focusedLabelColor = ClimateGreen
                    )
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isCreating,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CarbonGray
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            // Get location coordinates from locationState
                            val (lat, lng) = when (locationState) {
                                is LocationState.Success -> {
                                    Pair(locationState.location.latitude, locationState.location.longitude)
                                }
                                else -> Pair(0.0, 0.0) // Fallback values
                            }
                            onCreateProject(name, description, status, lat, lng, cropType, expectedYield)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isCreating &&
                                name.isNotBlank() &&
                                locationState is LocationState.Success, // Only enable if location is available
                        colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create Project")
                        }
                    }
                }

                // Helper text for location requirement
                if (locationState !is LocationState.Success) {
                    Text(
                        text = "Location detection is required to create a project",
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProjectDialog(
    project: ProjectResponse,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onUpdateProject: (String, String, String, String, Double, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var description by remember { mutableStateOf(project.description ?: "") }
    var status by remember { mutableStateOf(project.status) }
    var latitude by remember { mutableStateOf(project.location?.coordinates?.lat?.toString() ?: "") }
    var longitude by remember { mutableStateOf(project.location?.coordinates?.lng?.toString() ?: "") }
    var cropType by remember { mutableStateOf(project.location?.crop_type ?: "") }
    var expectedYield by remember { mutableStateOf(project.location?.expected_yield ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val statusOptions = listOf(
        "active" to "Active",
        "inactive" to "Inactive",
        "completed" to "Completed"
    )

    Dialog(onDismissRequest = { if (!isUpdating) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = EnergyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "Update Project",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isUpdating
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Project Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    leadingIcon = {
                        Icon(Icons.Default.Assignment, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EnergyBlue,
                        focusedLabelColor = EnergyBlue
                    )
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    minLines = 2,
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EnergyBlue,
                        focusedLabelColor = EnergyBlue
                    )
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isUpdating) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = statusOptions.find { it.first == status }?.second ?: "Active",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Status") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isUpdating,
                        leadingIcon = {
                            Icon(
                                when (status) {
                                    "active" -> Icons.Default.PlayCircle
                                    "completed" -> Icons.Default.CheckCircle
                                    else -> Icons.Default.PauseCircle
                                },
                                contentDescription = null,
                                tint = when (status) {
                                    "active" -> SuccessGreen
                                    "completed" -> EnergyBlue
                                    else -> WarningOrange
                                }
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EnergyBlue,
                            focusedLabelColor = EnergyBlue
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    status = value
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        when (value) {
                                            "active" -> Icons.Default.PlayCircle
                                            "completed" -> Icons.Default.CheckCircle
                                            else -> Icons.Default.PauseCircle
                                        },
                                        contentDescription = null,
                                        tint = when (value) {
                                            "active" -> SuccessGreen
                                            "completed" -> EnergyBlue
                                            else -> WarningOrange
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // Location Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EnergyBlue,
                            focusedLabelColor = EnergyBlue
                        )
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EnergyBlue,
                            focusedLabelColor = EnergyBlue
                        )
                    )
                }

                // Crop Type
                OutlinedTextField(
                    value = cropType,
                    onValueChange = { cropType = it },
                    label = { Text("Crop Type") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    leadingIcon = {
                        Icon(Icons.Default.Grass, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EnergyBlue,
                        focusedLabelColor = EnergyBlue
                    )
                )

                // Expected Yield
                OutlinedTextField(
                    value = expectedYield,
                    onValueChange = { expectedYield = it },
                    label = { Text("Expected Yield") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating,
                    leadingIcon = {
                        Icon(Icons.Default.Agriculture, contentDescription = null)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EnergyBlue,
                        focusedLabelColor = EnergyBlue
                    )
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CarbonGray
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val lat = latitude.toDoubleOrNull() ?: 0.0
                            val lng = longitude.toDoubleOrNull() ?: 0.0
                            onUpdateProject(project.id, name, description, status, lat, lng, cropType, expectedYield)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating && name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyBlue)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update Project")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteProjectDialog(
    project: ProjectResponse,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onDeleteProject: (String) -> Unit
) {
    Dialog(onDismissRequest = { if (!isDeleting) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
                            .background(Color(0xFFE57373).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        "Delete Project",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray
                    )
                }

                // Warning message
                Text(
                    "Are you sure you want to delete \"${project.name}\"?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CarbonGray
                )

                Text(
                    "This action cannot be undone. All project data will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )

                // Project info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                        if (!project.description.isNullOrBlank()) {
                            Text(
                                project.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Status: ${project.status.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                            project.location?.crop_type?.let { cropType ->
                                Text(
                                    "Crop: ${cropType.replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CarbonGray
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onDeleteProject(project.id) },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE57373)
                        )
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Delete Project")
                        }
                    }
                }
            }
        }
    }
}