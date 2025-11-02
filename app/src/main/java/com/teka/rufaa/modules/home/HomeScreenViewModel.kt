package com.teka.rufaa.modules.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import com.teka.rufaa.utils.offline_sync_utils.AutoSyncManager
import com.teka.rufaa.utils.offline_sync_utils.SyncStatus
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
    val isUserLoggedIn: Boolean = false,
    val syncStatus: SyncStatus? = null
)

/**
 * ViewModel for Home Screen
 * Manages user data retrieval, sync functionality, and logout
 */
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val appContext: Context,
    private val dataStoreRepository: DataStoreRepository,
    private val autoSyncManager: AutoSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    // Expose sync status as a separate flow for the banner
    private val _syncStatus = MutableStateFlow<SyncStatus?>(null)
    val syncStatus: StateFlow<SyncStatus?> = _syncStatus.asStateFlow()

    init {
        loadUserData()
        observeLoginState()
        updateCurrentDate()
        startAutoSync()
        loadSyncStatus()
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
     * Start auto-sync monitoring
     */
    private fun startAutoSync() {
        viewModelScope.launch {
            try {
                // Start monitoring network for auto-sync
                autoSyncManager.startMonitoring()

                // Schedule periodic sync as backup
                autoSyncManager.schedulePeriodicSync()

                Timber.tag("HomeVM").i("Auto-sync started")
            } catch (e: Exception) {
                Timber.tag("HomeVM").e("Failed to start auto-sync: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Load current sync status
     */
    private fun loadSyncStatus() {
        viewModelScope.launch {
            try {
                val status = autoSyncManager.getSyncStatus()
                _syncStatus.value = status
                _uiState.update { it.copy(syncStatus = status) }
                Timber.tag("HomeVM").i("Sync status loaded: ${status.unsyncedCount} unsynced items")
            } catch (e: Exception) {
                Timber.tag("HomeVM").e("Failed to load sync status: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Trigger manual sync
     * Called when user taps the sync button in the banner
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            try {
                Timber.tag("HomeVM").i("Manual sync triggered by user")

                // Show syncing state
                _syncStatus.value?.let { currentStatus ->
                    _syncStatus.value = currentStatus.copy(unsyncedCount = currentStatus.unsyncedCount)
                }

                // Trigger the sync
                autoSyncManager.triggerManualSync()

                // Reload sync status after a short delay to show updated count
                kotlinx.coroutines.delay(2000)
                loadSyncStatus()

            } catch (e: Exception) {
                Timber.tag("HomeVM").e("Manual sync failed: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(errorMessage = "Sync failed: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Refresh sync status - useful for pull-to-refresh or periodic updates
     */
    fun refreshSyncStatus() {
        loadSyncStatus()
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
     * Clears all user data from DataStore and stops sync
     */
    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Cancel all scheduled syncs
                autoSyncManager.cancelSync()

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
                        isLoading = false,
                        syncStatus = null
                    )
                }

                _syncStatus.value = null

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
        loadSyncStatus()
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

    /**
     * Get pending sync count for display
     */
    fun getPendingSyncCount(): Int {
        return _syncStatus.value?.unsyncedCount ?: 0
    }

    /**
     * Check if device is connected to network
     */
    fun isNetworkConnected(): Boolean {
        return _syncStatus.value?.isConnected ?: false
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel sync when ViewModel is cleared
        autoSyncManager.cancelSync()
        Timber.tag("HomeVM").i("HomeViewModel cleared, sync cancelled")
    }
}