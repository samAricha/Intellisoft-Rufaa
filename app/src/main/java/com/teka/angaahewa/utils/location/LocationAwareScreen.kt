package com.teka.angaahewa.utils.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LocationAwareScreen() {
    // Get the location state from your composable hook
    val locationState = useLocation()

    // Get the view model properly
    val locationViewModel: LocationViewModel = hiltViewModel()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, retry location fetch
            locationViewModel.retryLocationFetch()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (locationState) {
            is LocationState.Loading -> {
                CircularProgressIndicator()
                Text(
                    text = "Getting your location...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            is LocationState.Success -> {
                val location = locationState.location
                Text(
                    text = "Location Found!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text("Latitude: ${location.latitude}")
                Text("Longitude: ${location.longitude}")
                location.accuracy?.let { accuracy ->
                    Text("Accuracy: ${accuracy}m")
                }
                location.address?.let { address ->
                    Text(
                        text = "Address: $address",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            is LocationState.Error -> {
                Text(
                    text = "Failed to get location",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = locationState.message,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(
                    onClick = {
                        locationViewModel.retryLocationFetch()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Retry")
                }
            }

            is LocationState.PermissionDenied -> {
                Text(
                    text = "Location Permission Required",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Please grant location permission to continue",
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}