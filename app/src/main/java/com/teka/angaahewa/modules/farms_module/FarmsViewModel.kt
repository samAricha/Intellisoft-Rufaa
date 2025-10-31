    package com.teka.angaahewa.modules.farms_module

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
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

    @HiltViewModel
    class FarmsViewModel @Inject constructor(
        private val supabase: SupabaseClient
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(FarmsUiState())
        val uiState: StateFlow<FarmsUiState> = _uiState.asStateFlow()

        init {
            loadFarms()
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
                null
            }
        }

        fun loadFarms() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                try {
                    val currentProfile = getCurrentUserProfile()
                    if (currentProfile == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "User profile not found",
                                isAuthenticated = false
                            )
                        }
                        return@launch
                    }

                    val farms = supabase.from("farms")
                        .select {
                            filter {
                                eq("farmer_id", currentProfile.id)
                            }
                        }
                        .decodeList<FarmResponse>()


                    _uiState.update { currentState ->
                        currentState.copy(
                            farms = farms,
                            userProfile = currentProfile,
                            isLoading = false,
                            error = null,
                            isAuthenticated = true
                        )
                    }

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load farms: ${e.message}"
                        )
                    }
                }
            }
        }

        fun createFarm(
            address: String,
            latitude: Double,
            longitude: Double,
            city: String = "",
            state: String = "",
            acres: Int = 0,
            areaHectares: Double? = null
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isCreating = true, error = null) }

                try {
                    val currentProfile = getCurrentUserProfile()
                    if (currentProfile == null) {
                        _uiState.update {
                            it.copy(
                                isCreating = false,
                                error = "User profile not found"
                            )
                        }
                        return@launch
                    }

                    val farmLocation = FarmLocation(
                        latitude = latitude,
                        longitude = longitude,
                        address = address,
                        city = city.takeIf { it.isNotBlank() },
                        state = state.takeIf { it.isNotBlank() },
                        acres = acres,
                        area_hectares = areaHectares
                    )

                    val createRequest = CreateFarmRequest(
                        farmer_id = currentProfile.id,
                        location = farmLocation
                    )

                    supabase.from("farms")
                        .insert(createRequest)

                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            showCreateDialog = false
                        )
                    }

                    // Reload farms to show the new one
                    loadFarms()

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = "Failed to create farm: ${e.message}"
                        )
                    }
                }
            }
        }

        fun updateFarm(
            farmId: String,
            address: String,
            latitude: Double,
            longitude: Double,
            city: String = "",
            state: String = "",
            acres: Int = 0,
            areaHectares: Double? = null
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isUpdating = true, error = null) }

                try {
                    val farmLocation = FarmLocation(
                        latitude = latitude,
                        longitude = longitude,
                        address = address,
                        city = city.takeIf { it.isNotBlank() },
                        state = state.takeIf { it.isNotBlank() },
                        acres = acres,
                        area_hectares = areaHectares
                    )

                    val updateRequest = UpdateFarmRequest(location = farmLocation)

                    supabase.from("farms")
                        .update(updateRequest) {
                            filter {
                                eq("id", farmId)
                            }
                        }

                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            showUpdateDialog = false,
                            selectedFarm = null
                        )
                    }

                    // Reload farms to show the updated data
                    loadFarms()

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update farm: ${e.message}"
                        )
                    }
                }
            }
        }

        fun deleteFarm(farmId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isDeleting = true, error = null) }

                try {
                    supabase.from("farms")
                        .delete {
                            filter {
                                eq("id", farmId)
                            }
                        }

                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteDialog = false,
                            selectedFarm = null
                        )
                    }

                    // Reload farms to reflect the deletion
                    loadFarms()

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = "Failed to delete farm: ${e.message}"
                        )
                    }
                }
            }
        }

        fun getUserFarms(): List<FarmResponse> {
            val currentUserId = getCurrentUserId()
            val currentProfile = _uiState.value.userProfile

            return if (currentUserId != null && currentProfile != null) {
                _uiState.value.farms.filter { it.farmer_id == currentProfile.id }
            } else {
                emptyList()
            }
        }

        fun getFarmById(farmId: String): FarmResponse? {
            return _uiState.value.farms.find { it.id == farmId }
        }

        fun searchFarms(query: String) {
            val filteredFarms = if (query.isBlank()) {
                _uiState.value.allFarms
            } else {
                _uiState.value.allFarms.filter { farm ->
                    farm.location?.address?.contains(query, ignoreCase = true) == true ||
                    farm.profiles?.name?.contains(query, ignoreCase = true) == true ||
                    farm.location?.city?.contains(query, ignoreCase = true) == true ||
                    farm.location?.state?.contains(query, ignoreCase = true) == true
                }
            }

            _uiState.update {
                it.copy(
                    farms = filteredFarms,
                    searchQuery = query
                )
            }
        }

        fun clearSearch() {
            _uiState.update {
                it.copy(
                    farms = it.allFarms,
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

        fun showUpdateDialog(farm: FarmResponse) {
            _uiState.update {
                it.copy(
                    showUpdateDialog = true,
                    selectedFarm = farm
                )
            }
        }

        fun hideUpdateDialog() {
            _uiState.update {
                it.copy(
                    showUpdateDialog = false,
                    selectedFarm = null
                )
            }
        }

        fun showDeleteDialog(farm: FarmResponse) {
            _uiState.update {
                it.copy(
                    showDeleteDialog = true,
                    selectedFarm = farm
                )
            }
        }

        fun hideDeleteDialog() {
            _uiState.update {
                it.copy(
                    showDeleteDialog = false,
                    selectedFarm = null
                )
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun refreshFarms() {
            loadFarms()
        }

        // Get farms statistics
        fun getFarmStats(): FarmStats {
            val userFarms = getUserFarms()
            val totalAcres = userFarms.sumOf { it.location?.acres ?: 0 }
            val totalHectares = userFarms.sumOf { it.location?.area_hectares ?: 0.0 }

            return FarmStats(
                totalFarms = userFarms.size,
                totalAcres = totalAcres,
                totalHectares = totalHectares,
                averageSize = if (userFarms.isNotEmpty()) totalAcres.toDouble() / userFarms.size else 0.0
            )
        }

        // Calculate total carbon credits from all user farms
        fun getTotalCarbonCredits(): Double {
            val userFarms = getUserFarms()
            return userFarms.sumOf { farm ->
                val acres = farm.location?.acres ?: 0
                acres * 2.3 // tons of CO2 per acre (you can adjust this formula)
            }
        }

        // Calculate total carbon credit value
        fun getTotalCreditValue(): Double {
            val totalCredits = getTotalCarbonCredits()
            val creditValuePerTon = 15.0 // USD per ton
            return totalCredits * creditValuePerTon
        }
    }

    data class FarmsUiState(
        val farms: List<FarmResponse> = emptyList(),
        val allFarms: List<FarmResponse> = emptyList(),
        val userProfile: ProfileResponse? = null,
        val selectedFarm: FarmResponse? = null,
        val isLoading: Boolean = false,
        val isCreating: Boolean = false,
        val isUpdating: Boolean = false,
        val isDeleting: Boolean = false,
        val error: String? = null,
        val isAuthenticated: Boolean = false,
        val searchQuery: String = "",
        val showCreateDialog: Boolean = false,
        val showUpdateDialog: Boolean = false,
        val showDeleteDialog: Boolean = false
    )

    data class FarmStats(
        val totalFarms: Int,
        val totalAcres: Int,
        val totalHectares: Double,
        val averageSize: Double
    )