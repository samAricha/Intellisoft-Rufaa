package com.teka.angaahewa.modules.image_geotagging

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.angaahewa.modules.farms_module.ProfileResponse
import com.teka.angaahewa.modules.home_module.ProjectResponse
import com.teka.angaahewa.utils.location.LocationState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GeotaggedImageViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "GeotaggedImageViewModel"
        private const val STORAGE_BUCKET = "dmrv-images"
    }

    private val _uiState = MutableStateFlow(GeotaggedImageUiState())
    val uiState: StateFlow<GeotaggedImageUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "GeotaggedImageViewModel initialized")
        loadUserProjects()
    }

    private fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    private suspend fun getCurrentUserProfile(): ProfileResponse? {
        val currentUserId = getCurrentUserId() ?: return null
        return try {
            supabase.from("profiles")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<ProfileResponse>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile", e)
            null
        }
    }

    private fun loadUserProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjects = true) }

            try {
                val currentProfile = getCurrentUserProfile()
                if (currentProfile == null) {
                    _uiState.update {
                        it.copy(
                            isLoadingProjects = false,
                            error = "Profile not found"
                        )
                    }
                    return@launch
                }

                val projects = supabase.from("projects")
                    .select {
                        filter {
                            eq("farmer_id", currentProfile.id)
                        }
                    }
                    .decodeList<ProjectResponse>()

                _uiState.update {
                    it.copy(
                        projects = projects,
                        userProfile = currentProfile,
                        isLoadingProjects = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading projects", e)
                _uiState.update {
                    it.copy(
                        isLoadingProjects = false,
                        error = "Failed to load projects: ${e.message}"
                    )
                }
            }
        }
    }

    fun setSelectedProject(project: ProjectResponse?) {
        _uiState.update { it.copy(selectedProject = project) }
        getImagesByProject(project?.id ?: "")
    }

    fun setImageType(type: ImageType) {
        _uiState.update { it.copy(selectedImageType = type) }
    }

    fun setDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun setLocationState(locationState: LocationState) {
        _uiState.update { it.copy(locationState = locationState) }
    }

    fun captureGeotaggedImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val currentState = _uiState.value
                val locationState = currentState.locationState
                val selectedProject = currentState.selectedProject
                val selectedImageType = currentState.selectedImageType
                val description = currentState.description

                // Validate required data
                if (selectedProject == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Please select a project"
                        )
                    }
                    return@launch
                }

                if (locationState !is LocationState.Success) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Location not available. Please enable GPS and try again."
                        )
                    }
                    return@launch
                }

                val location = locationState.location

                // Get current user ID
/*
                val currentUserId = getCurrentUserId()
                if (currentUserId == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "User not authenticated"
                        )
                    }
                    return@launch
                }
*/


                // Add GPS EXIF data to the image
                val updatedUri = addGpsExifData(imageUri, location.latitude, location.longitude)
                if (updatedUri == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to add GPS data to image"
                        )
                    }
                    return@launch
                }


                // Upload image to Supabase Storage
                val imageBytes = context.contentResolver.openInputStream(updatedUri)?.readBytes()
                if (imageBytes == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to read image data"
                        )
                    }
                    return@launch
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "${selectedProject.id}_${selectedImageType.name.lowercase()}_${timestamp}.jpg"
                val imagePath = "projects/${selectedProject.id}/$fileName"


                // Upload to Supabase Storage
                try {
                    val uploadResult = supabase.storage
                        .from(STORAGE_BUCKET)
                        .upload(imagePath, imageBytes) {
                            upsert = false
                        }

                    Log.d(TAG, "Image uploaded successfully: $imagePath")
                } catch (e: Exception) {
                    Log.e(TAG, "Storage upload error", e)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to upload image: ${e.message}"
                        )
                    }
                    return@launch
                }


                Log.d(TAG, "Image uploaded successfully: $imagePath")

                // Get public URL
                val publicUrl = supabase.storage
                    .from(STORAGE_BUCKET)
                    .publicUrl(imagePath)

                // Save metadata to database
                val imageRecord = GeotaggedImageRecord(
                    project_id = selectedProject.id,
                    image_type = selectedImageType.name.lowercase(),
                    image_url = publicUrl,
                    image_path = imagePath,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy?.toDouble(),
                    address = location.address,
                    description = description.takeIf { it.isNotBlank() },
                    captured_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                    farmer_id = currentState.userProfile?.id ?: ""
                )

                supabase.from("geotagged_images")
                    .insert(imageRecord)

                Log.d(TAG, "Image metadata saved successfully")

                // Add to recent images
                val recentImages = _uiState.value.recentImages.toMutableList()
                recentImages.add(0, imageRecord)
                if (recentImages.size > 10) {
                    recentImages.removeAt(recentImages.size - 1)
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        recentImages = recentImages,
                        description = "", // Clear description after successful save
                        lastCapturedImage = imageRecord,
                        showSuccessDialog = true
                    )
                }

                Log.d(TAG, "Geotagged image capture completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error capturing geotagged image", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save geotagged image: ${e.message}"
                    )
                }
            }
        }
    }


    private fun addGpsExifData(imageUri: Uri, latitude: Double, longitude: Double): Uri? {
        return try {
            // Create a temporary file to work with
            val tempFile =
                File(context.cacheDir, "temp_geotagged_${System.currentTimeMillis()}.jpg")

            // Copy the image from URI to temporary file
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Now work with the file path for EXIF
            val exif = ExifInterface(tempFile.absolutePath)

            // Add GPS coordinates
            exif.setLatLong(latitude, longitude)

            // Add timestamp
            val timestamp = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
            exif.setAttribute(ExifInterface.TAG_DATETIME, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, timestamp)

            // Add camera info
            exif.setAttribute(ExifInterface.TAG_MAKE, "DMRV App")
            exif.setAttribute(ExifInterface.TAG_MODEL, "Climate Monitor")
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Angaa Hewa DMRV")

            // Save the EXIF data to the temporary file
            exif.saveAttributes()

            // Copy the modified file back to the original URI
            context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Clean up temporary file
            tempFile.delete()

            Log.d(TAG, "Successfully added GPS EXIF data to image")
            imageUri

        } catch (e: Exception) {
            Log.e(TAG, "Error adding GPS EXIF data", e)
            null
        }
    }


    fun loadRecentImages() {
        viewModelScope.launch {
            try {
                val currentProfile = getCurrentUserProfile() ?: return@launch

                val recentImages = supabase.from("geotagged_images")
                    .select {
                        filter {
                            eq("farmer_id", currentProfile.id)
                        }
                        order("captured_at", Order.DESCENDING)
                        limit(10)
                    }
                    .decodeList<GeotaggedImageRecord>()

                _uiState.update {
                    it.copy(recentImages = recentImages)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading recent images", e)
            }
        }
    }

    fun getImagesByProject(projectId: String) {
        Log.d(TAG, "Fetching images for project: $projectId")
        viewModelScope.launch {
            try {
                val images = supabase.from("geotagged_images")
                    .select {
                        filter {
                            eq("project_id", projectId)
                        }
                        order("captured_at", Order.DESCENDING)
                    }
                    .decodeList<GeotaggedImageRecord>()

                _uiState.update {
                    it.copy(projectImages = images)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading project images", e)
            }
        }
    }

    fun validateLocation(): Boolean {
        val locationState = _uiState.value.locationState
        if (locationState !is LocationState.Success) {
            _uiState.update {
                it.copy(error = "GPS location is required for geotagged images")
            }
            return false
        }

        val location = locationState.location
        val selectedProject = _uiState.value.selectedProject

        // Validate if location is within reasonable distance of project location
        selectedProject?.location?.let { projectLocation ->
            val distance = calculateDistance(
                location.latitude, location.longitude,
                projectLocation.coordinates?.lat ?: 0.0, projectLocation.coordinates?.lng ?: 0.0
            )

            // If more than 1km away, show warning but don't block
            if (distance > 1000) {
                Log.w(TAG, "Current location is ${distance}m away from project location")
                _uiState.update {
                    it.copy(
                        locationWarning = "You are ${String.format("%.0f", distance)}m away from the project location. Are you sure this image belongs to this project?"
                    )
                }
            }
        }

        return true
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearLocationWarning() {
        _uiState.update { it.copy(locationWarning = null) }
    }

    fun hideSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun refreshData() {
        loadUserProjects()
        loadRecentImages()
    }
}

data class GeotaggedImageUiState(
    val projects: List<ProjectResponse> = emptyList(),
    val selectedProject: ProjectResponse? = null,
    val selectedImageType: ImageType = ImageType.FIELD_CONDITION,
    val description: String = "",
    val locationState: LocationState = LocationState.Loading,
    val recentImages: List<GeotaggedImageRecord> = emptyList(),
    val projectImages: List<GeotaggedImageRecord> = emptyList(),
    val capturedImages: List<Bitmap> = emptyList(),
    val userProfile: ProfileResponse? = null,
    val isLoadingProjects: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val locationWarning: String? = null,
    val lastCapturedImage: GeotaggedImageRecord? = null,
    val showSuccessDialog: Boolean = false
)

@Serializable
data class GeotaggedImageRecord(
    val id: String = "",
    val project_id: String,
    val farmer_id: String,
    val image_type: String,
    val image_url: String,
    val image_path: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val address: String? = null,
    val description: String? = null,
    val captured_at: String,
    val created_at: String = "",
    val updated_at: String = ""
)

enum class ImageType(val displayName: String, val description: String) {
    FIELD_CONDITION("Field Condition", "General field state and crop condition"),
    CROP_GROWTH("Crop Growth", "Crop development stages and growth"),
    PEST_DAMAGE("Pest/Disease", "Pest infestation or disease damage"),
    WEATHER_DAMAGE("Weather Impact", "Weather-related damage or conditions"),
    HARVEST("Harvest", "Harvest activities and yield documentation"),
    EQUIPMENT("Equipment", "Farming equipment and tools used"),
    SOIL_CONDITION("Soil", "Soil quality and condition assessment"),
    WATER_SOURCE("Water Management", "Irrigation and water usage"),
    CONSERVATION_PRACTICE("Conservation", "Climate-smart farming practices"),
    BOUNDARY_VERIFICATION("Boundary", "Project boundary verification"),
    MONITORING_EQUIPMENT("Monitoring Tools", "Environmental monitoring equipment"),
    OTHER("Other", "Other project-related documentation")
}