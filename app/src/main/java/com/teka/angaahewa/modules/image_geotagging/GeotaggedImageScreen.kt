package com.teka.angaahewa.modules.image_geotagging

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.teka.angaahewa.modules.home_module.ProjectResponse
import com.teka.angaahewa.ui.theme.CarbonGray
import com.teka.angaahewa.ui.theme.ClimateGreen
import com.teka.angaahewa.ui.theme.EnergyBlue
import com.teka.angaahewa.ui.theme.SuccessGreen
import com.teka.angaahewa.ui.theme.WarningOrange
import com.teka.angaahewa.utils.location.LocationState
import com.teka.angaahewa.utils.location.LocationViewModel
import com.teka.angaahewa.utils.location.useLocation
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
// Add the missing imports at the top of your file
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeotaggedImageScreen(
    navController: NavController,
    viewModel: GeotaggedImageViewModel = hiltViewModel(),
    locationViewModel: LocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationState = useLocation()
    val context = LocalContext.current

    // Add state for captured images preview
    var capturedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Image capture launcher with preview handling
    val imageCaptureContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            try {
                // Convert URI to Bitmap for preview
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri!!))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri!!)
                }

                // Add to preview list
                capturedImages = capturedImages + bitmap

                // Just capture - let the ViewModel handle validation
                viewModel.captureGeotaggedImage(imageUri!!)
            } catch (e: Exception) {
                Log.e("ImageCapture", "Error loading captured image", e)
            }
        }
    }



    // Update location state in viewModel
    LaunchedEffect(locationState) {
        viewModel.setLocationState(locationState)
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create URI and launch camera immediately after permission is granted
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "DMRV_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }

            val photoUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            photoUri?.let { uri ->
                imageUri = uri
                imageCaptureContract.launch(uri)
            }
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            locationViewModel.retryLocationFetch()
        }
    }

    // Function to launch camera
    fun launchCamera() {
        // First check camera permission
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                // Check location availability
                when (locationState) {
                    is LocationState.PermissionDenied -> {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return
                    }
                    is LocationState.Loading, is LocationState.Error -> {
                        return
                    }
                    is LocationState.Success -> {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "DMRV_${System.currentTimeMillis()}.jpg")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        }

                        val photoUri = context.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )

                        photoUri?.let { uri ->
                            imageUri = uri
                            imageCaptureContract.launch(uri)
                        }
                    }
                }
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Scaffold(
        topBar = {
            GeotaggedImageTopBar()
        },
        floatingActionButton = {
            if (uiState.selectedProject != null && locationState is LocationState.Success) {
                FloatingActionButton(
                    onClick = { launchCamera() },
                    containerColor = ClimateGreen,
                    contentColor = Color.White
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Geotagged Image"
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FFFE)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoadingProjects -> LoadingContent("Loading projects...")
                uiState.error != null -> ErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.refreshData() }
                )
                uiState.projects.isEmpty() -> EmptyProjectsState()
                else -> GeotaggedImageContent(
                    uiState = uiState,
                    locationState = locationState,
                    capturedImages = capturedImages,
                    onProjectSelected = { viewModel.setSelectedProject(it) },
                    onImageTypeSelected = { viewModel.setImageType(it) },
                    onDescriptionChanged = { viewModel.setDescription(it) },
                    onRetryLocation = { locationViewModel.retryLocationFetch() },
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onRemoveImage = { index ->
                        capturedImages = capturedImages.toMutableList().apply { removeAt(index) }
                    },
                    onClearAllImages = {
                        capturedImages = emptyList()
                    }
                )
            }
        }
    }

    // Success Dialog
    if (uiState.showSuccessDialog && uiState.lastCapturedImage != null) {
        ImageCaptureSuccessDialog(
            image = uiState.lastCapturedImage!!,
            onDismiss = {
                viewModel.hideSuccessDialog()
                // Clear captured images after successful save
                capturedImages = emptyList()
            }
        )
    }

    // Location Warning Dialog
    uiState.locationWarning?.let { warning ->
        LocationWarningDialog(
            warning = warning,
            onProceed = {
                viewModel.clearLocationWarning()
                imageUri?.let { viewModel.captureGeotaggedImage(it) }
            },
            onCancel = {
                viewModel.clearLocationWarning()
                // Remove the preview image since user cancelled
                capturedImages = capturedImages.dropLast(1)
            }
        )
    }

    // Handle errors with snackbar or similar
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
}

@Composable
fun GeotaggedImageContent(
    uiState: GeotaggedImageUiState,
    locationState: LocationState,
    capturedImages: List<Bitmap>,
    onProjectSelected: (ProjectResponse?) -> Unit,
    onImageTypeSelected: (ImageType) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onRetryLocation: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onClearAllImages: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Project Selection
        item {
            ProjectSelectionSection(
                projects = uiState.projects,
                selectedProject = uiState.selectedProject,
                onProjectSelected = onProjectSelected
            )
        }

        // Location Status
        item {
            LocationStatusSection(
                locationState = locationState,
                selectedProject = uiState.selectedProject,
                onRetryLocation = onRetryLocation,
                onRequestLocationPermission = onRequestLocationPermission
            )
        }

        // Captured Images Preview (similar to chat screen)
        if (capturedImages.isNotEmpty()) {
            item {
                CapturedImagesPreview(
                    images = capturedImages,
                    onRemoveImage = onRemoveImage,
                    onClearAll = onClearAllImages
                )
            }
        }

        // Image Type Selection (only show if project is selected and location is available)
        if (uiState.selectedProject != null && locationState is LocationState.Success) {
            item {
                ImageTypeSelectionSection(
                    selectedImageType = uiState.selectedImageType,
                    onImageTypeSelected = onImageTypeSelected
                )
            }

            item {
                ImageDescriptionSection(
                    description = uiState.description,
                    onDescriptionChanged = onDescriptionChanged
                )
            }
        }

        // Recent Images
        if (uiState.recentImages.isNotEmpty()) {
            item {
                RecentImagesSection(
                    images = uiState.recentImages
                )
            }
        }

        // Extra spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CapturedImagesPreview(
    images: List<Bitmap>,
    onRemoveImage: (Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = images.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Captured Images (${images.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray
                )

                if (images.size > 1) {
                    TextButton(
                        onClick = onClearAll,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ClimateGreen.copy(alpha = 0.05f)
                )
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = images,
                        key = { index, _ -> index }
                    ) { index, bitmap ->
                        CapturedImageItem(
                            bitmap = bitmap,
                            onRemove = { onRemoveImage(index) }
                        )
                    }
                }
            }

            // Info text
            Text(
                text = "These images will be geotagged with your current GPS location and saved to the selected project.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun CapturedImageItem(
    bitmap: Bitmap,
    onRemove: () -> Unit
) {
    Box {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured image preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Remove button (similar to chat screen)
        Surface(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp)
                .size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
            shadowElevation = 6.dp
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                tint = Color.White
            )
        }

        // GPS indicator badge
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-4).dp, y = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = SuccessGreen.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
                Text(
                    text = "GPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeotaggedImageTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = ClimateGreen,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        "Geotagged Images",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ClimateGreen
                    )
                    Text(
                        "DMRV Documentation",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectionSection(
    projects: List<ProjectResponse>,
    selectedProject: ProjectResponse?,
    onProjectSelected: (ProjectResponse?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Select Project",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedProject?.name ?: "Choose a project",
                onValueChange = { },
                readOnly = true,
                label = { Text("Project") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                leadingIcon = {
                    Icon(Icons.Default.Assignment, contentDescription = null)
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
                projects.forEach { project ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    project.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    project.status.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (project.status) {
                                        "active" -> SuccessGreen
                                        "completed" -> EnergyBlue
                                        else -> WarningOrange
                                    }
                                )
                            }
                        },
                        onClick = {
                            onProjectSelected(project)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                when (project.status) {
                                    "active" -> Icons.Default.PlayCircle
                                    "completed" -> Icons.Default.CheckCircle
                                    else -> Icons.Default.PauseCircle
                                },
                                contentDescription = null,
                                tint = when (project.status) {
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
    }
}

@Composable
fun LocationStatusSection(
    locationState: LocationState,
    selectedProject: ProjectResponse?,
    onRetryLocation: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "GPS Location Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )

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
                            text = "Getting GPS location...",
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
                            text = "GPS Location Acquired",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${String.format("%.6f", location.latitude)}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    text = "Latitude",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF888888)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${String.format("%.6f", location.longitude)}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    text = "Longitude",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF888888)
                                )
                            }
                            location.accuracy?.let { accuracy ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format("%.0f", accuracy)}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "Accuracy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }
                        }
                        
                        location.address?.let { address ->
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Show distance from project location if available
                        selectedProject?.location?.let { projectLocation ->
                            if (projectLocation.coordinates?.lat != null && projectLocation.coordinates.lng != null) {
                                val distance = calculateDistance(
                                    location.latitude, location.longitude,
                                    projectLocation.coordinates?.lat ?: 0.0, projectLocation.coordinates?.lng ?: 0.0
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Straighten,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (distance > 1000) WarningOrange else SuccessGreen
                                    )
                                    Text(
                                        text = "${String.format("%.0f", distance)}m from project center",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (distance > 1000) WarningOrange else SuccessGreen
                                    )
                                }
                            }
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
                            text = "GPS Error",
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
                            onClick = onRetryLocation,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry GPS")
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
                            text = "GPS location is required for geotagged images to ensure data integrity",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onRequestLocationPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enable GPS")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageTypeSelectionSection(
    selectedImageType: ImageType,
    onImageTypeSelected: (ImageType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Image Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ImageType.values()) { imageType ->
                FilterChip(
                    selected = selectedImageType == imageType,
                    onClick = { onImageTypeSelected(imageType) },
                    label = { 
                        Text(
                            imageType.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            getImageTypeIcon(imageType),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // Show description for selected type
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ClimateGreen.copy(alpha = 0.05f))
        ) {
            Text(
                text = selectedImageType.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun ImageDescriptionSection(
    description: String,
    onDescriptionChanged: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Description (Optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Add details about this image") },
            placeholder = { Text("e.g., Crop showing signs of pest damage on lower leaves") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = null)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ClimateGreen,
                focusedLabelColor = ClimateGreen
            )
        )
    }
}

@Composable
fun RecentImagesSection(
    images: List<GeotaggedImageRecord>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Recent Images",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images) { image ->
                RecentImageItem(image = image)
            }
        }
    }
}

@Composable
fun RecentImageItem(image: GeotaggedImageRecord) {
    Card(
        modifier = Modifier.size(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            AsyncImage(
                model = image.image_url,
                contentDescription = image.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = image.image_type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ClimateGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatImageDate(image.captured_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ImageCaptureSuccessDialog(
    image: GeotaggedImageRecord,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Image Captured Successfully!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray,
                    textAlign = TextAlign.Center
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoRow("Type", image.image_type.replaceFirstChar { it.uppercase() })
                    InfoRow("GPS", "${String.format("%.6f", image.latitude)}, ${String.format("%.6f", image.longitude)}")
                    image.accuracy?.let { accuracy ->
                        InfoRow("Accuracy", "${String.format("%.0f", accuracy)}m")
                    }
                    image.address?.let { address ->
                        InfoRow("Location", address)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
fun LocationWarningDialog(
    warning: String,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Location Warning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray
                    )
                }

                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onProceed,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                    ) {
                        Text("Proceed")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = CarbonGray
        )
    }
}

@Composable
fun LoadingContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = ClimateGreen,
                strokeWidth = 4.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFE57373)
            )
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

@Composable
fun EmptyProjectsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF999999)
            )
            Text(
                text = "No Projects Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = "Create a project first to start capturing geotagged images for DMRV",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions
fun getImageTypeIcon(imageType: ImageType): ImageVector {
    return when (imageType) {
        ImageType.FIELD_CONDITION -> Icons.Default.Grass
        ImageType.CROP_GROWTH -> Icons.Default.Agriculture
        ImageType.PEST_DAMAGE -> Icons.Default.BugReport
        ImageType.WEATHER_DAMAGE -> Icons.Default.Storm
        ImageType.HARVEST -> Icons.Default.Agriculture
        ImageType.EQUIPMENT -> Icons.Default.Build
        ImageType.SOIL_CONDITION -> Icons.Default.Terrain
        ImageType.WATER_SOURCE -> Icons.Default.Water
        ImageType.CONSERVATION_PRACTICE -> Icons.Default.Eco
        ImageType.BOUNDARY_VERIFICATION -> Icons.Default.Map
        ImageType.MONITORING_EQUIPMENT -> Icons.Default.Sensors
        ImageType.OTHER -> Icons.Default.CameraAlt
    }
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

fun formatImageDate(dateString: String): String {
    return try {
        val parts = dateString.split("T")[0].split("-")
        "${parts[2]}/${parts[1]}"
    } catch (e: Exception) {
        "Today"
    }
}