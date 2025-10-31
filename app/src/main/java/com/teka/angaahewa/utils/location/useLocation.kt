package com.teka.angaahewa.utils.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.teka.angaahewa.core.MainActivity



@Composable
fun useLocation(): LocationState {
    val context = LocalContext.current
    val locationViewModel = hiltViewModel<LocationViewModel>()
    val locationState by locationViewModel.locationState.collectAsState()

    // Track permission state to trigger re-composition when permissions change
    var permissionRequested by remember { mutableStateOf(false) }

    val hasPermissions = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasPermissions) { // React to permission changes
        if (hasPermissions) {
            locationViewModel.onPermissionResult(true)
        } else if (!permissionRequested) {
            permissionRequested = true
            locationViewModel.onPermissionResult(false) // Set to PermissionDenied state
            (context as? MainActivity)?.requestLocationPermissions()
        }
    }

    return locationState
}