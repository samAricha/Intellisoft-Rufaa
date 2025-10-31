package com.teka.angaahewa.modules.home_module

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val id: String,
    val user_id: String,
    val name: String,
    val email: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class UserRoleResponse(
    val id: String,
    val user_id: String,
    val role: String, // Should be "farmer" for farmers
    val created_at: String? = null
)

@Serializable
data class FarmResponse(
    val id: String,
    val farmer_id: String,
    val location: FarmLocation? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val profiles: ProfileResponse? = null // For joined queries
)

@Serializable
data class FarmLocation(
    val city: String? = null,
    val state: String? = null,
    val acres: Int? = null,
    val coordinates: Coordinates? = null,
    val address: String? = null // Computed from city and state
) {
    fun getDisplayAddress(): String {
        return when {
            !city.isNullOrBlank() && !state.isNullOrBlank() -> "$city, $state"
            !city.isNullOrBlank() -> city
            !state.isNullOrBlank() -> state
            !address.isNullOrBlank() -> address
            else -> "Location not specified"
        }
    }
    
    fun getAcreageText(): String {
        return acres?.let { "$it acres" } ?: "Acreage not specified"
    }
}

@Serializable
data class Coordinates(
    val lat: Double,
    val lng: Double
)

@Serializable
data class ProjectResponse(
    val id: String,
    val farmer_id: String,
    val name: String,
    val description: String,
    val status: String, // "active", "inactive", "completed", etc.
    val location: ProjectLocation? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val profiles: ProfileResponse? = null // For joined queries
)

@Serializable
data class ProjectLocation(
    val area: String? = null,
    val crop_type: String? = null,
    val coordinates: Coordinates? = null, // Changed from flat lat/lng
    val expected_yield: String? = null,
    val coverage_acres: Int? = null
) {
    fun getDisplayInfo(): String {
        val parts = mutableListOf<String>()

        area?.let { parts.add("Area: $it") }
        crop_type?.let { parts.add("Crop: $it") }
        expected_yield?.let { parts.add("Expected yield: $it") }
        coverage_acres?.let { parts.add("Coverage: $it acres") }

        return if (parts.isNotEmpty()) parts.joinToString(" • ") else ""
    }
}


data class GreenEnergyProduct(
    val name: String,
    val description: String,
    val costPerUnit: Double,
    val unit: String,
    val icon: ImageVector,
    val category: String
)

data class CarbonCreditValue(
    val totalCredits: Double,
    val creditValuePerTon: Double,
    val totalValue: Double
)