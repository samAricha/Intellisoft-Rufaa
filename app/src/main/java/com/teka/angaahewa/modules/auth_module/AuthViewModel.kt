package com.teka.angaahewa.modules.auth_module

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.angaahewa.core.navigation.AUTH_GRAPH_ROUTE
import com.teka.angaahewa.core.navigation.To_MAIN_GRAPH_ROUTE
import com.teka.angaahewa.data_layer.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    val applicationContext: Context,
    private val supabase: SupabaseClient
) : ViewModel() {

    private var _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()


    init {
        viewModelScope.launch {
            updateStartDestination()
        }
    }


    fun logout() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
                clearUserData()
                dataStoreRepository.clearUserData()

                // Optionally update UI state
                _startDestination.value = AUTH_GRAPH_ROUTE

            } catch (e: Exception) {
                Log.e("LogoutError", "Error during logout: ${e.message}", e)
                // Even if Supabase logout fails, clear local data
                clearUserData()
                dataStoreRepository.clearUserData()
                _startDestination.value = AUTH_GRAPH_ROUTE
            }
        }
    }


    suspend fun clearUserData() {
        dataStoreRepository.clearUserData()
    }

    private suspend fun updateStartDestination() {
        supabase.auth.sessionStatus.collectLatest { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    _startDestination.value = To_MAIN_GRAPH_ROUTE
                }
                is SessionStatus.NotAuthenticated -> {
                    _startDestination.value = AUTH_GRAPH_ROUTE
                }

                SessionStatus.Initializing -> {}
                is SessionStatus.RefreshFailure -> {}
            }
            _isLoading.value = false
        }
    }


    fun loadFieldAgentFromDataStore() {
        viewModelScope.launch {
            dataStoreRepository.getFieldAgentData().collect { fieldAgent ->
//                _fieldAgent.value = fieldAgent
            }
        }
    }

    val isFieldAgent: StateFlow<Boolean> = dataStoreRepository.isFieldAgent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}
