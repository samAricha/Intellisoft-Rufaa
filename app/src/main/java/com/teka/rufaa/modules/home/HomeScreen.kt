package com.teka.rufaa.modules.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.teka.rufaa.core.navigation.AppScreens
import com.teka.rufaa.utils.offline_sync_utils.SyncStatusBanner
import com.teka.rufaa.utils.offline_sync_utils.SyncStatus
import com.teka.rufaa.R

/**
 * Home screen composable - Main navigation hub for the app
 * Provides access to patient registration, listing, and settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    val syncStatus: SyncStatus? by viewModel.syncStatus.collectAsState()

    // Show error message if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            // Error will be shown in Snackbar
        }
    }

    // Handle logout completion
    val handleLogout = {
        viewModel.logout {
            // Navigate to login and clear back stack
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                userName = uiState.userName,
                currentDate = uiState.currentDate
            )
        },
        snackbarHost = {
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.errorMessage ?: "")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            // Sync Status Banner
            syncStatus?.let { status ->
                SyncStatusBanner(
                    syncStatus = status,
                    onManualSyncClick = {
                        viewModel.triggerManualSync()
                    }
                )
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Loading indicator
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Quick Actions Section
                Text(
                    text = "Quick Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Register Patient Card
                NavigationCard(
                    title = "Register New Patient",
                    description = "Add a new patient to the system",
                    icon = Icons.Default.PersonAdd,
                    gradientColors = listOf(
                        Color(0xFF66BB6A),
                        Color(0xFF4CAF50)
                    ),
                    onClick = { navController.navigate("patient_registration") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // View Patients Card
                NavigationCard(
                    title = "View Patients",
                    description = "Browse and manage patient records",
                    icon = Icons.Default.People,
                    gradientColors = listOf(
                        Color(0xFF42A5F5),
                        Color(0xFF2196F3)
                    ),
                    onClick = { navController.navigate(AppScreens.PatientsListScreen.route) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Settings Card
                NavigationCard(
                    title = "Settings",
                    description = "App preferences and configuration",
                    icon = Icons.Default.Settings,
                    gradientColors = listOf(
                        Color(0xFF78909C),
                        Color(0xFF607D8B)
                    ),
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Button
                OutlinedButton(
                    onClick = handleLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF44336), Color(0xFFF44336))
                        )
                    ),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFF44336),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Logout",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Top app bar with welcome message and current date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(userName: String, currentDate: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2196F3),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(top = 16.dp)
        ) {
            // App Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                        .padding(0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rufaa_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rufaa Care",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Welcome Message
            Text(
                text = if (userName.isNotEmpty()) {
                    "Welcome back, $userName!"
                } else {
                    "Welcome to Patient Management"
                },
                fontSize = 16.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Current Date
            Text(
                text = currentDate,
                fontSize = 14.sp,
                color = Color(0xFFE0E0E0)
            )
        }
    }
}

/**
 * Reusable navigation card component
 */
@Composable
fun NavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Text Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color(0xFFE0E0E0)
                    )
                }

                // Arrow Icon
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Navigate",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}