package com.teka.angaahewa.modules.farms_module

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.teka.angaahewa.ui.theme.CarbonGray
import com.teka.angaahewa.ui.theme.ClimateGreen
import com.teka.angaahewa.ui.theme.EnergyBlue
import com.teka.angaahewa.ui.theme.SkyBlue
import com.teka.angaahewa.ui.theme.SolarYellow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmsScreen(
    navController: NavController,
    viewModel: FarmsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLocationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FarmsTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.searchFarms(it) },
                onClearSearch = { viewModel.clearSearch() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = ClimateGreen,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Farm"
                )
            }
        },
        containerColor = Color(0xFFF8FFFE)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> FarmsLoadingContent()
                uiState.error != null -> FarmsErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.refreshFarms() }
                )
                uiState.farms.isEmpty() && uiState.searchQuery.isBlank() -> FarmsEmptyState(
                    onCreateFarm = { viewModel.showCreateDialog() }
                )
                uiState.farms.isEmpty() && uiState.searchQuery.isNotBlank() -> FarmsEmptySearchState(
                    searchQuery = uiState.searchQuery,
                    onClearSearch = { viewModel.clearSearch() }
                )
                else -> FarmsContent(
                    farms = uiState.farms,
                    farmStats = viewModel.getFarmStats(),
                    totalCarbonCredits = viewModel.getTotalCarbonCredits(),
                    totalCreditValue = viewModel.getTotalCreditValue(),
                    onEditFarm = { farm -> viewModel.showUpdateDialog(farm) },
                    onDeleteFarm = { farm -> viewModel.showDeleteDialog(farm) }
                )
            }
        }
    }

    // Dialogs
    if (uiState.showCreateDialog) {
        CreateFarmDialog(
            isCreating = uiState.isCreating,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreateFarm = { address, lat, lng, city, state, acres, hectares ->
                viewModel.createFarm(address, lat, lng, city, state, acres, hectares)
            },
            onLocationRequest = { showLocationDialog = true }
        )
    }

    if (uiState.showUpdateDialog && uiState.selectedFarm != null) {
        UpdateFarmDialog(
            farm = uiState.selectedFarm!!,
            isUpdating = uiState.isUpdating,
            onDismiss = { viewModel.hideUpdateDialog() },
            onUpdateFarm = { farmId, address, lat, lng, city, state, acres, hectares ->
                viewModel.updateFarm(farmId, address, lat, lng, city, state, acres, hectares)
            }
        )
    }

    if (uiState.showDeleteDialog && uiState.selectedFarm != null) {
        DeleteFarmDialog(
            farm = uiState.selectedFarm!!,
            isDeleting = uiState.isDeleting,
            onDismiss = { viewModel.hideDeleteDialog() },
            onDeleteFarm = { farmId ->
                viewModel.deleteFarm(farmId)
            }
        )
    }

    // Error handling
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            // Auto-clear error after some time
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmsTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = null,
                    tint = ClimateGreen,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        "Carbon Sites",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ClimateGreen
                    )
                    Text(
                        "Manage monitoring locations",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun FarmsContent(
    farms: List<FarmResponse>,
    farmStats: FarmStats,
    totalCarbonCredits: Double,
    totalCreditValue: Double,
    onEditFarm: (FarmResponse) -> Unit,
    onDeleteFarm: (FarmResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Farm Statistics Section
        item {
            FarmStatsSection(
                stats = farmStats,
                totalCarbonCredits = totalCarbonCredits,
                totalCreditValue = totalCreditValue
            )
        }

        // Farms List
        item {
            ClimateSectionHeader("Your Carbon Sites", Icons.Default.Forest)
        }

        items(farms) { farm ->
            FarmCard(
                farm = farm,
                onEdit = { onEditFarm(farm) },
                onDelete = { onDeleteFarm(farm) }
            )
        }

        // Extra spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun FarmStatsSection(
    stats: FarmStats,
    totalCarbonCredits: Double,
    totalCreditValue: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Carbon Impact Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )

        // First row of stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClimateStatCard(
                modifier = Modifier.weight(1f),
                title = "Total Sites",
                value = stats.totalFarms.toString(),
                subtitle = "Monitoring",
                icon = Icons.Default.LocationOn,
                backgroundColor = ClimateGreen
            )
            ClimateStatCard(
                modifier = Modifier.weight(1f),
                title = "Total Area",
                value = "${stats.totalAcres}",
                subtitle = "Acres",
                icon = Icons.Default.Landscape,
                backgroundColor = EnergyBlue
            )
        }

        // Second row of stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClimateStatCard(
                modifier = Modifier.weight(1f),
                title = "Carbon Credits",
                value = String.format("%.1f", totalCarbonCredits),
                subtitle = "Tons CO₂",
                icon = Icons.Default.Eco,
                backgroundColor = SolarYellow
            )
            ClimateStatCard(
                modifier = Modifier.weight(1f),
                title = "Credit Value",
                value = "KSH${String.format("%.0f", totalCreditValue)}",
                subtitle = "Estimated",
                icon = Icons.Default.AttachMoney,
                backgroundColor = SkyBlue
            )
        }
    }
}

@Composable
fun ClimateStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(backgroundColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = backgroundColor
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ClimateSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ClimateGreen,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmCard(
    farm: FarmResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val acres = farm.location?.acres ?: 0
    val carbonCredits = acres * 2.3 // tons of CO2
    val creditValue = carbonCredits * 15.0 // USD

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ClimateGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ClimateGreen
                        )
                    }
                    Column {
                        Text(
                            text = farm.profiles?.name ?: "Carbon Site",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                        Text(
                            text = "Monitoring Location",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit farm",
                            modifier = Modifier.size(18.dp),
                            tint = EnergyBlue
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete farm",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFE57373)
                        )
                    }
                }
            }

            // Location info
            if (farm.location?.address != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ClimateGreen
                    )
                    Text(
                        text = farm.location.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FarmMetricItem(
                    label = "Area",
                    value = "$acres acres",
                    icon = Icons.Default.Landscape,
                    color = ClimateGreen
                )
                FarmMetricItem(
                    label = "Carbon Credits",
                    value = "${String.format("%.1f", carbonCredits)}t",
                    icon = Icons.Default.Eco,
                    color = EnergyBlue
                )
                FarmMetricItem(
                    label = "Est. Value",
                    value = "KSH${String.format("%.0f", creditValue)}",
                    icon = Icons.Default.AttachMoney,
                    color = SolarYellow
                )
            }
        }
    }
}

@Composable
fun FarmMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF888888)
        )
    }
}

@Composable
fun FarmsEmptyState(
    onCreateFarm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ClimateGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ClimateGreen
                )
            }
            Text(
                text = "No Carbon Sites Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = "Start monitoring your carbon impact by adding your first carbon sequestration site",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onCreateFarm,
                colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Carbon Site")
            }
        }
    }
}

@Composable
fun FarmsEmptySearchState(
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF999999)
            )
            Text(
                text = "No Results Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = "No farms found matching \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            TextButton(
                onClick = onClearSearch,
                colors = ButtonDefaults.textButtonColors(contentColor = ClimateGreen)
            ) {
                Text("Clear Search")
            }
        }
    }
}

@Composable
fun FarmsLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = ClimateGreen,
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading carbon sites...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun FarmsErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFE57373)
            )
            Text(
                text = "Connection Issue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}