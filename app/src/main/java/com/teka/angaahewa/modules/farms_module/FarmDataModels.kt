package com.teka.angaahewa.modules.farms_module

import kotlinx.serialization.Serializable

@Serializable
data class FarmLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String? = null,
    val state: String? = null,
    val acres: Int = 0,
    val area_hectares: Double? = null,
    val coordinates: Coordinates? = null
)

@Serializable
data class Coordinates(
    val lat: Double,
    val lng: Double
)

@Serializable
data class FarmResponse(
    val id: String,
    val farmer_id: String,
    val location: FarmLocation? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val profiles: ProfileResponse? = null
)

@Serializable
data class CreateFarmRequest(
    val farmer_id: String,
    val location: FarmLocation
)

@Serializable
data class UpdateFarmRequest(
    val location: FarmLocation? = null
)

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
    val role: String,
    val created_at: String? = null
)

@Serializable
data class ProjectResponse(
    val id: String,
    val farmer_id: String,
    val name: String,
    val description: String,
    val status: String,
    val location: ProjectLocation? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class ProjectLocation(
    val address: String? = null,
    val coordinates: Coordinates? = null
)

// Additional data classes for the app
data class CarbonCreditValue(
    val totalCredits: Double,
    val creditValuePerTon: Double,
    val totalValue: Double
)

data class GreenEnergyProduct(
    val name: String,
    val description: String,
    val costPerUnit: Double,
    val unit: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val category: String
)