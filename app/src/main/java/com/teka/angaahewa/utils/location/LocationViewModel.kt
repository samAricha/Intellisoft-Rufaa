package com.teka.angaahewa.utils.location

import android.location.LocationManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Loading)
    val locationState = _locationState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            fetchCurrentLocation()
        } else {
            _locationState.value = LocationState.PermissionDenied
        }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = LocationState.Loading
            _locationState.value = locationRepository.getCurrentLocation()
        }
    }

    fun retryLocationFetch() {
        fetchCurrentLocation()
    }
}

