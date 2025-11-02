package com.teka.rufaa.modules.auth_module


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.core.navigation.AUTH_GRAPH_ROUTE
import com.teka.rufaa.core.navigation.To_MAIN_GRAPH_ROUTE
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private var repository: DataStoreRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination


    init {
        viewModelScope.launch {
            updateStartDestination()
        }
    }

    private suspend fun updateStartDestination() {
        repository.isUserLoggedIn.collectLatest { isLoggedIn ->
            if (isLoggedIn) {
                _startDestination.value = To_MAIN_GRAPH_ROUTE
            } else {
                _startDestination.value = AUTH_GRAPH_ROUTE
            }
        }
        _isLoading.value = false
    }




}