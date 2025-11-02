package com.teka.rufaa.modules.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State for Home Screen
 */
data class HomeScreenUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userId: Int? = null,
    val currentDate: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
)

/**
 * ViewModel for Home Screen
 * Manages user data retrieval and logout functionality
 */
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val appContext: Context,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        observeLoginState()
        updateCurrentDate()
    }

    /**
     * Load user data from DataStore
     */
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Collect user data
                dataStoreRepository.getLoggedInUserData().collectLatest { userData ->
                    if (userData != null) {
                        _uiState.update {
                            it.copy(
                                userName = userData.name,
                                userEmail = userData.email,
                                userId = userData.id,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        Timber.tag("HomeVM").i("Loaded user data: ${userData.name}")
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "User data not found"
                            )
                        }
                        Timber.tag("HomeVM").w("No user data found in DataStore")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load user data: ${e.localizedMessage}"
                    )
                }
                Timber.tag("HomeVM").e("Error loading user data: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Observe login state to handle automatic logout if session expires
     */
    private fun observeLoginState() {
        viewModelScope.launch {
            dataStoreRepository.isUserLoggedIn.collectLatest { isLoggedIn ->
                _uiState.update { it.copy(isUserLoggedIn = isLoggedIn) }
                
                if (!isLoggedIn) {
                    Timber.tag("HomeVM").i("User is not logged in")
                }
            }
        }
    }

    /**
     * Update current date in the UI state
     */
    private fun updateCurrentDate() {
        val dateFormat = java.text.SimpleDateFormat(
            "EEEE, MMMM dd, yyyy",
            java.util.Locale.getDefault()
        )
        val currentDate = dateFormat.format(java.util.Date())
        _uiState.update { it.copy(currentDate = currentDate) }
    }

    /**
     * Get user name for display
     * Returns empty string if not available
     */
    fun getUserName(): String {
        return _uiState.value.userName
    }

    /**
     * Get user email for display
     */
    fun getUserEmail(): String {
        return _uiState.value.userEmail
    }

    /**
     * Check if user has valid access token
     */
    suspend fun hasValidAccessToken(): Boolean {
        return try {
            dataStoreRepository.hasValidAccessToken()
        } catch (e: Exception) {
            Timber.tag("HomeVM").e("Error checking access token: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Get complete user profile information
     */
    suspend fun getUserProfileInfo(): String {
        return try {
            dataStoreRepository.getUserProfileInfo()
        } catch (e: Exception) {
            Timber.tag("HomeVM").e("Error getting profile info: ${e.localizedMessage}")
            "Profile information unavailable"
        }
    }

    /**
     * Perform logout operation
     * Clears all user data from DataStore
     */
    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Clear all user data
                dataStoreRepository.clearUserData()
                
                // Set logged in state to false
                dataStoreRepository.saveLoggedInState(false)
                
                Timber.tag("HomeVM").i("User logged out successfully")
                
                _uiState.update {
                    it.copy(
                        userName = "",
                        userEmail = "",
                        userId = null,
                        isUserLoggedIn = false,
                        isLoading = false
                    )
                }
                
                // Callback to navigate to login screen
                onLogoutComplete()
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Logout failed: ${e.localizedMessage}"
                    )
                }
                Timber.tag("HomeVM").e("Logout error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Refresh user data manually
     */
    fun refreshUserData() {
        loadUserData()
        updateCurrentDate()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Get access token (useful for API calls)
     */
    fun getAccessToken(): Flow<String> {
        return dataStoreRepository.getAccessToken
    }

    /**
     * Check if user data is loaded
     */
    fun isUserDataLoaded(): Boolean {
        return _uiState.value.userName.isNotEmpty() && _uiState.value.userId != null
    }
}