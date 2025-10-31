package com.teka.angaahewa.modules.project_modules

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.angaahewa.modules.farms_module.ProfileResponse
import com.teka.angaahewa.modules.home_module.Coordinates
import com.teka.angaahewa.modules.home_module.ProjectLocation
import com.teka.angaahewa.modules.home_module.ProjectResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.serialization.Serializable



@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    companion object {
        private const val TAG = "ProjectsViewModel"
    }

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ProjectsViewModel initialized")
        loadProjects()
    }

    private fun getCurrentUserId(): String? {
        val userId = supabase.auth.currentUserOrNull()?.id
        Log.d(TAG, "getCurrentUserId: $userId")
        return userId
    }

    private suspend fun getCurrentUserProfile(): ProfileResponse? {
        val currentUserId = getCurrentUserId() ?: return null
        Log.d(TAG, "getCurrentUserProfile: Fetching profile for userId: $currentUserId")

        return try {
            val profile = supabase.from("profiles")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<ProfileResponse>()

            Log.d(TAG, "getCurrentUserProfile: Profile found - ID: ${profile?.id}, Name: ${profile?.name}")
            profile
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserProfile: Error fetching profile", e)
            null
        }
    }

    fun loadProjects() {
        Log.d(TAG, "loadProjects: Starting to load projects")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val currentUserId = getCurrentUserId()
                Log.d(TAG, "loadProjects: Current user ID: $currentUserId")

                if (currentUserId == null) {
                    Log.w(TAG, "loadProjects: User not authenticated")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "User not authenticated",
                            isAuthenticated = false
                        )
                    }
                    return@launch
                }

                // Get profile first to use profile.id as farmer_id
                val currentProfile = getCurrentUserProfile()
                Log.d(TAG, "loadProjects: Current profile ID: ${currentProfile?.id}")

                if (currentProfile == null) {
                    Log.w(TAG, "loadProjects: Profile not found for current user")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Profile not found for current user",
                            isAuthenticated = false
                        )
                    }
                    return@launch
                }

                Log.d(TAG, "loadProjects: Querying projects with farmer_id = ${currentProfile.id}")

                // Use profile.id as farmer_id and include profiles relation
                val projects = supabase.from("projects")
                    .select {
                        filter {
                            eq("farmer_id", currentProfile.id)
                        }
                    }
                    .decodeList<ProjectResponse>()

                Log.d(TAG, "loadProjects: Query completed. Found ${projects.size} projects")

                // Log each project for debugging
                projects.forEachIndexed { index, project ->
                    Log.d(TAG, "loadProjects: Project $index - ID: ${project.id}, Name: ${project.name}, FarmerID: ${project.farmer_id}, Status: ${project.status}")
                }

                _uiState.update { currentState ->
                    val newState = currentState.copy(
                        projects = projects,
                        allProjects = projects,
                        userProfile = currentProfile,
                        isLoading = false,
                        error = null,
                        isAuthenticated = true
                    )
                    Log.d(TAG, "loadProjects: UI State updated - Projects: ${newState.projects.size}, IsLoading: ${newState.isLoading}, IsAuthenticated: ${newState.isAuthenticated}")
                    newState
                }

            } catch (e: Exception) {
                Log.e(TAG, "loadProjects: Error loading projects", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load projects: ${e.message}"
                    )
                }
            }
        }
    }

    fun createProject(
        name: String,
        description: String,
        status: String,
        latitude: Double,
        longitude: Double,
        cropType: String = "",
        expectedYield: String = ""
    ) {
        Log.d(TAG, "createProject: Creating project - Name: $name, Status: $status")
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            try {
                val currentProfile = getCurrentUserProfile()
                Log.d(TAG, "createProject: Current profile ID: ${currentProfile?.id}")

                if (currentProfile == null) {
                    Log.w(TAG, "createProject: Profile not found")
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = "Profile not found"
                        )
                    }
                    return@launch
                }

                val projectLocation = ProjectLocation(
                    coordinates = Coordinates(lat = latitude, lng = longitude), // Changed
                    crop_type = cropType.takeIf { it.isNotBlank() },
                    expected_yield = expectedYield.takeIf { it.isNotBlank() }
                )

                val createRequest = CreateProjectRequest(
                    farmer_id = currentProfile.id,
                    name = name,
                    description = description,
                    status = status,
                    location = projectLocation
                )

                Log.d(TAG, "createProject: Inserting project with farmer_id: ${createRequest.farmer_id}")

                supabase.from("projects")
                    .insert(createRequest)

                Log.d(TAG, "createProject: Project created successfully")

                _uiState.update {
                    it.copy(
                        isCreating = false,
                        showCreateDialog = false
                    )
                }

                loadProjects()

            } catch (e: Exception) {
                Log.e(TAG, "createProject: Error creating project", e)
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        error = "Failed to create project: ${e.message}"
                    )
                }
            }
        }
    }


    fun getUserProjects(): List<ProjectResponse> {
        val currentProfile = _uiState.value.userProfile
        Log.d(TAG, "getUserProjects: Filtering projects for profile ID: ${currentProfile?.id}")

        val userProjects = if (currentProfile != null) {
            _uiState.value.projects.filter { it.farmer_id == currentProfile.id }
        } else {
            emptyList()
        }

        Log.d(TAG, "getUserProjects: Found ${userProjects.size} projects for current user")
        return userProjects
    }

    // Debug method to check what's in the database
    fun debugDatabaseQuery() {
        Log.d(TAG, "debugDatabaseQuery: Starting debug query")
        viewModelScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                Log.d(TAG, "debugDatabaseQuery: Current user ID: $currentUserId")

                // Query all projects to see what's in the database
                val allProjects = supabase.from("projects")
                    .select()
                    .decodeList<ProjectResponse>()

                Log.d(TAG, "debugDatabaseQuery: Total projects in database: ${allProjects.size}")
                allProjects.forEachIndexed { index, project ->
                    Log.d(TAG, "debugDatabaseQuery: Project $index - ID: ${project.id}, FarmerID: ${project.farmer_id}, Name: ${project.name}")
                }

                // Query profiles to check structure
                val currentProfile = getCurrentUserProfile()
                Log.d(TAG, "debugDatabaseQuery: Current profile - ID: ${currentProfile?.id}, UserID: ${currentProfile?.user_id}")

                // Query projects with profile.id if it exists
                if (currentProfile != null) {
                    val projectsWithProfileId = supabase.from("projects")
                        .select {
                            filter {
                                eq("farmer_id", currentProfile.id)
                            }
                        }
                        .decodeList<ProjectResponse>()
                    Log.d(TAG, "debugDatabaseQuery: Projects with profile.id (${currentProfile.id}): ${projectsWithProfileId.size}")
                }

                // Query projects with currentUserId
                if (currentUserId != null) {
                    val projectsWithUserId = supabase.from("projects")
                        .select {
                            filter {
                                eq("farmer_id", currentUserId)
                            }
                        }
                        .decodeList<ProjectResponse>()
                    Log.d(TAG, "debugDatabaseQuery: Projects with user.id ($currentUserId): ${projectsWithUserId.size}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "debugDatabaseQuery: Error in debug query", e)
            }
        }
    }

    fun updateProject(
        projectId: String,
        name: String,
        description: String,
        status: String,
        latitude: Double,
        longitude: Double,
        cropType: String = "",
        expectedYield: String = ""
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }

            try {
                val projectLocation = ProjectLocation(
                    coordinates = Coordinates(lat = latitude, lng = longitude), // Changed
                    crop_type = cropType.takeIf { it.isNotBlank() },
                    expected_yield = expectedYield.takeIf { it.isNotBlank() }
                )

                val updateRequest = UpdateProjectRequest(
                    name = name,
                    description = description,
                    status = status,
                    location = projectLocation
                )

                supabase.from("projects")
                    .update(updateRequest) {
                        filter {
                            eq("id", projectId)
                        }
                    }

                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        showUpdateDialog = false,
                        selectedProject = null
                    )
                }

                loadProjects()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update project: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateProjectStatus(projectId: String, status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }

            try {
                val updateRequest = mapOf("status" to status)

                supabase.from("projects")
                    .update(updateRequest) {
                        filter {
                            eq("id", projectId)
                        }
                    }

                _uiState.update {
                    it.copy(isUpdating = false)
                }

                // Reload projects to show the updated status
                loadProjects()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update project status: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }

            try {
                supabase.from("projects")
                    .delete {
                        filter {
                            eq("id", projectId)
                        }
                    }

                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        showDeleteDialog = false,
                        selectedProject = null
                    )
                }

                // Reload projects to reflect the deletion
                loadProjects()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete project: ${e.message}"
                    )
                }
            }
        }
    }

    fun getProjectById(projectId: String): ProjectResponse? {
        return _uiState.value.projects.find { it.id == projectId }
    }

    fun getProjectsByStatus(status: String): List<ProjectResponse> {
        return getUserProjects().filter { it.status == status }
    }

    fun searchProjects(query: String) {
        val filteredProjects = if (query.isBlank()) {
            _uiState.value.allProjects
        } else {
            _uiState.value.allProjects.filter { project ->
                project.name.contains(query, ignoreCase = true) ||
                        project.description?.contains(query, ignoreCase = true) == true ||
                        project.location?.crop_type?.contains(query, ignoreCase = true) == true ||
                        project.profiles?.name?.contains(query, ignoreCase = true) == true
            }
        }

        _uiState.update {
            it.copy(
                projects = filteredProjects,
                searchQuery = query
            )
        }
    }

    fun filterProjectsByStatus(status: String?) {
        val filteredProjects = if (status == null) {
            _uiState.value.allProjects
        } else {
            _uiState.value.allProjects.filter { it.status == status }
        }

        _uiState.update {
            it.copy(
                projects = filteredProjects,
                selectedStatusFilter = status
            )
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                projects = it.allProjects,
                searchQuery = ""
            )
        }
    }

    // UI State management methods
    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun showUpdateDialog(project: ProjectResponse) {
        _uiState.update {
            it.copy(
                showUpdateDialog = true,
                selectedProject = project
            )
        }
    }

    fun hideUpdateDialog() {
        _uiState.update {
            it.copy(
                showUpdateDialog = false,
                selectedProject = null
            )
        }
    }

    fun showDeleteDialog(project: ProjectResponse) {
        _uiState.update {
            it.copy(
                showDeleteDialog = true,
                selectedProject = project
            )
        }
    }

    fun hideDeleteDialog() {
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                selectedProject = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refreshProjects() {
        loadProjects()
    }

    // Get project statistics
    fun getProjectStats(): ProjectStats {
        val userProjects = getUserProjects()
        val activeProjects = userProjects.count { it.status == "active" }
        val inactiveProjects = userProjects.count { it.status == "inactive" }
        val completedProjects = userProjects.count { it.status == "completed" }

        return ProjectStats(
            totalProjects = userProjects.size,
            activeProjects = activeProjects,
            inactiveProjects = inactiveProjects,
            completedProjects = completedProjects
        )
    }

    // Calculate total expected yield from all user projects
    fun getTotalExpectedYield(): Double {
        val userProjects = getUserProjects()
        return userProjects.sumOf { project ->
            project.location?.expected_yield?.let { yieldStr ->
                yieldStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            } ?: 0.0
        }
    }

    // Get crop type distribution
    fun getCropTypeDistribution(): Map<String, Int> {
        val userProjects = getUserProjects()
        return userProjects.mapNotNull { it.location?.crop_type }
            .groupingBy { it }
            .eachCount()
    }
}


data class ProjectsUiState(
    val projects: List<ProjectResponse> = emptyList(),
    val allProjects: List<ProjectResponse> = emptyList(),
    val userProfile: ProfileResponse? = null,
    val selectedProject: ProjectResponse? = null,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val showCreateDialog: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val showDeleteDialog: Boolean = false
)

@Serializable
data class ProjectStats(
    val totalProjects: Int,
    val activeProjects: Int,
    val inactiveProjects: Int,
    val completedProjects: Int
)

@Serializable
data class CreateProjectRequest(
    val farmer_id: String,
    val name: String,
    val description: String,
    val status: String,
    val location: ProjectLocation
)

@Serializable
data class UpdateProjectRequest(
    val name: String,
    val description: String,
    val status: String,
    val location: ProjectLocation
)