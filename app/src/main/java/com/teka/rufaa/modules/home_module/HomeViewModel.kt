package com.teka.rufaa.modules.home_module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun updateUiState(
        update: HomeUiState.() -> HomeUiState
    ) {
        _uiState.update { it.update() }
    }

    // Get current user ID from Supabase Auth
    private fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Get current logged-in user ID
                val currentUserId = getCurrentUserId()

                if (currentUserId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated",
                        isAuthenticated = false
                    )
                    return@launch
                }

                // Step 1: Get farmer profile using current user ID
                val userProfile = supabase.from("profiles")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeSingleOrNull<ProfileResponse>()

                if (userProfile == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Profile not found for current user"
                    )
                    return@launch
                }

                // Step 2: Get user role
                val userRole = supabase.from("user_roles")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeSingleOrNull<UserRoleResponse>()

                // Step 3: Get farms using profile ID as farmer_id
                val userFarms = supabase.from("farms")
                    .select {
                        filter {
                            eq("farmer_id", userProfile.id)
                        }
                    }
                    .decodeList<FarmResponse>()

                // Step 4: Get projects using profile ID as farmer_id
                val userProjects = supabase.from("projects")
                    .select {
                        filter {
                            eq("farmer_id", userProfile.id)
                        }
                    }
                    .decodeList<ProjectResponse>()

                _uiState.value = HomeUiState(
                    profile = userProfile,
                    userRole = userRole,
                    farms = userFarms,
                    projects = userProjects,
                    isLoading = false,
                    error = null,
                    isAuthenticated = true,
                    userId = currentUserId
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load dashboard: ${e.message}",
                    isAuthenticated = false
                )
            }
        }
    }

    fun refreshData() {
        loadDashboard()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Get user email from profile
    fun getUserEmail(): String? {
        return _uiState.value.profile?.email
    }

    // Check if user is authenticated
    fun isUserAuthenticated(): Boolean {
        return _uiState.value.isAuthenticated && getCurrentUserId() != null
    }

    fun getUserDisplayName(): String? {
        return _uiState.value.profile?.name
    }

    // Get dashboard stats
    fun getDashboardStats(): DashboardStats {
        val state = _uiState.value
        return DashboardStats(
            totalFarms = state.farms.size,
            totalProjects = state.projects.size,
            activeFarms = state.farms.size,
            activeProjects = state.projects.count { it.status == "active" }
        )
    }

    // Get active projects count
    fun getActiveProjectsCount(): Int {
        return _uiState.value.projects.count { it.status == "active" }
    }

    // Get total farm acreage
    fun getTotalAcreage(): Int {
        return _uiState.value.farms.sumOf { it.location?.acres ?: 0 }
    }

    // Sign out user
    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                _uiState.value = HomeUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to sign out: ${e.message}"
                )
            }
        }
    }

    // Get active projects only
    suspend fun getActiveProjectsOnly(): List<ProjectResponse> {
        return try {
            val currentUserId = getCurrentUserId() ?: return emptyList()

            val userProfile = supabase.from("profiles")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<ProfileResponse>()

            if (userProfile != null) {
                supabase.from("projects")
                    .select {
                        filter {
                            eq("farmer_id", userProfile.id)
                            eq("status", "active")
                        }
                    }
                    .decodeList<ProjectResponse>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get projects with farm info using joins
    suspend fun getProjectsWithFarmInfo(): List<ProjectResponse> {
        return try {
            val currentUserId = getCurrentUserId() ?: return _uiState.value.projects

            val userProfile = supabase.from("profiles")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<ProfileResponse>()

            if (userProfile != null) {
                supabase.from("projects")
                    .select {
                        filter {
                            eq("farmer_id", userProfile.id)
                        }
                    }
                    .decodeList<ProjectResponse>()
            } else {
                _uiState.value.projects
            }
        } catch (e: Exception) {
            _uiState.value.projects
        }
    }

    // Update project status
    suspend fun updateProjectStatus(projectId: String, newStatus: String): Boolean {
        return try {
            supabase.from("projects")
                .update(
                    mapOf("status" to newStatus)
                ) {
                    filter {
                        eq("id", projectId)
                    }
                }

            // Refresh data after update
            loadDashboard()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Create new project
    suspend fun createProject(
        name: String,
        description: String,
        status: String = "active",
        location: ProjectLocation? = null
    ): Boolean {
        return try {
            val currentUserId = getCurrentUserId() ?: return false

            val userProfile = supabase.from("profiles")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<ProfileResponse>()

            if (userProfile != null) {
                val newProject = mapOf(
                    "farmer_id" to userProfile.id,
                    "name" to name,
                    "description" to description,
                    "status" to status,
                    "location" to location
                )

                supabase.from("projects")
                    .insert(newProject)

                // Refresh data after creation
                loadDashboard()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Update farm location
    suspend fun updateFarmLocation(
        farmId: String,
        city: String,
        state: String,
        acres: Int,
        coordinates: Coordinates? = null
    ): Boolean {
        return try {
            val locationUpdate = mutableMapOf<String, Any>(
                "city" to city,
                "state" to state,
                "acres" to acres
            )

            coordinates?.let {
                locationUpdate["coordinates"] = it
            }

            supabase.from("farms")
                .update(
                    mapOf("location" to locationUpdate)
                ) {
                    filter {
                        eq("id", farmId)
                    }
                }

            // Refresh data after update
            loadDashboard()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get complete dashboard data with joins
    suspend fun getCompleteDashboardData(): Boolean {
        return try {
            val currentUserId = getCurrentUserId() ?: return false

            // Get profile first
            val userProfile = supabase.from("profiles")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<ProfileResponse>()

            // Then get farms separately
            val farms = supabase.from("farms")
                .select {
                    filter {
                        eq("farmer_id", currentUserId)
                    }
                }
                .decodeList<FarmResponse>()

            // And projects separately
            val projects = supabase.from("projects")
                .select {
                    filter {
                        eq("farmer_id", currentUserId)
                    }
                }
                .decodeList<ProjectResponse>()

            true
        } catch (e: Exception) {
            false
        }
    }
}

data class HomeUiState(
    val profile: ProfileResponse? = null,
    val userRole: UserRoleResponse? = null,
    val farms: List<FarmResponse> = emptyList(),
    val projects: List<ProjectResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val showLogoutDialog: Boolean = false
)

data class DashboardStats(
    val totalFarms: Int = 0,
    val totalProjects: Int = 0,
    val activeFarms: Int = 0,
    val activeProjects: Int = 0
)

// Helper data class for complete dashboard query with joins
@kotlinx.serialization.Serializable
data class ProfileWithRelations(
    val id: String,
    val user_id: String,
    val name: String,
    val email: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val farms: List<FarmResponse>? = null,
    val projects: List<ProjectResponse>? = null
)