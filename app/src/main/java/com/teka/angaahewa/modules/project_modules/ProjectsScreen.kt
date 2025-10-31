package com.teka.angaahewa.modules.project_modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.teka.angaahewa.modules.home_module.ProjectResponse
import com.teka.angaahewa.ui.theme.CarbonGray
import com.teka.angaahewa.ui.theme.ClimateGreen
import com.teka.angaahewa.ui.theme.EnergyBlue
import com.teka.angaahewa.ui.theme.SolarYellow
import com.teka.angaahewa.ui.theme.SuccessGreen
import com.teka.angaahewa.ui.theme.WarningOrange


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ProjectsTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.searchProjects(it) },
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
                    contentDescription = "Add Project"
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
                uiState.isLoading -> ProjectsLoadingContent()
                uiState.error != null -> ProjectsErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.refreshProjects() }
                )
                uiState.projects.isEmpty() && uiState.searchQuery.isBlank() -> ProjectsEmptyState(
                    onCreateProject = { viewModel.showCreateDialog() }
                )
                uiState.projects.isEmpty() && uiState.searchQuery.isNotBlank() -> ProjectsEmptySearchState(
                    searchQuery = uiState.searchQuery,
                    onClearSearch = { viewModel.clearSearch() }
                )
                else -> ProjectsContent(
                    projects = uiState.projects,
                    projectStats = viewModel.getProjectStats(),
                    selectedStatusFilter = uiState.selectedStatusFilter,
                    totalExpectedYield = viewModel.getTotalExpectedYield(),
                    cropDistribution = viewModel.getCropTypeDistribution(),
                    onEditProject = { project -> viewModel.showUpdateDialog(project) },
                    onDeleteProject = { project -> viewModel.showDeleteDialog(project) },
                    onStatusFilterChange = { status -> viewModel.filterProjectsByStatus(status) },
                    onStatusUpdate = { projectId, status -> viewModel.updateProjectStatus(projectId, status) }
                )
            }
        }
    }

    // Dialogs
    if (uiState.showCreateDialog) {
        CreateProjectDialog(
            isCreating = uiState.isCreating,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreateProject = { name, description, status, lat, lng, cropType, expectedYield ->
                viewModel.createProject(name, description, status, lat, lng, cropType, expectedYield)
            }
        )
    }

    if (uiState.showUpdateDialog && uiState.selectedProject != null) {
        UpdateProjectDialog(
            project = uiState.selectedProject!!,
            isUpdating = uiState.isUpdating,
            onDismiss = { viewModel.hideUpdateDialog() },
            onUpdateProject = { projectId, name, description, status, lat, lng, cropType, expectedYield ->
                viewModel.updateProject(projectId, name, description, status, lat, lng, cropType, expectedYield)
            }
        )
    }

    if (uiState.showDeleteDialog && uiState.selectedProject != null) {
        DeleteProjectDialog(
            project = uiState.selectedProject!!,
            isDeleting = uiState.isDeleting,
            onDismiss = { viewModel.hideDeleteDialog() },
            onDeleteProject = { projectId ->
                viewModel.deleteProject(projectId)
            }
        )
    }

    // Error handling
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsTopBar(
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
                    imageVector = Icons.Default.WorkOutline,
                    contentDescription = null,
                    tint = ClimateGreen,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        "Carbon Projects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ClimateGreen
                    )
                    Text(
                        "Manage your farming projects",
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
fun ProjectsContent(
    projects: List<ProjectResponse>,
    projectStats: ProjectStats,
    selectedStatusFilter: String?,
    totalExpectedYield: Double,
    cropDistribution: Map<String, Int>,
    onEditProject: (ProjectResponse) -> Unit,
    onDeleteProject: (ProjectResponse) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onStatusUpdate: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Project Statistics Section
        item {
            ProjectStatsSection(
                stats = projectStats,
                totalExpectedYield = totalExpectedYield,
                cropDistribution = cropDistribution
            )
        }

        // Status Filter Section
        item {
            StatusFilterSection(
                selectedStatus = selectedStatusFilter,
                onStatusChange = onStatusFilterChange
            )
        }

        // Projects List Header
        item {
            ClimateSectionHeader("Your Projects", Icons.Default.Assignment)
        }

        // Projects List
        items(projects) { project ->
            ProjectCard(
                project = project,
                onEdit = { onEditProject(project) },
                onDelete = { onDeleteProject(project) },
                onStatusUpdate = { status -> onStatusUpdate(project.id, status) }
            )
        }

        // Extra spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ProjectStatsSection(
    stats: ProjectStats,
    totalExpectedYield: Double,
    cropDistribution: Map<String, Int>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Project Overview",
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
                title = "Total Projects",
                value = stats.totalProjects.toString(),
                subtitle = "Active",
                icon = Icons.Default.Assignment,
                backgroundColor = ClimateGreen
            )
            ClimateStatCard(
                modifier = Modifier.weight(1f),
                title = "Active",
                value = stats.activeProjects.toString(),
                subtitle = "Running",
                icon = Icons.Default.PlayCircle,
                backgroundColor = SuccessGreen
            )
        }

        // Second row of stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClimateStatCard(
                modifier = Modifier.weight(1f),
                title = "Expected Yield",
                value = "${String.format("%.1f", totalExpectedYield)}kg",
                subtitle = "Total",
                icon = Icons.Default.Agriculture,
                backgroundColor = SolarYellow
            )
            ClimateStatCard(
                modifier = Modifier.weight(1f),
                title = "Completed",
                value = stats.completedProjects.toString(),
                subtitle = "Projects",
                icon = Icons.Default.CheckCircle,
                backgroundColor = EnergyBlue
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
fun StatusFilterSection(
    selectedStatus: String?,
    onStatusChange: (String?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Filter by Status",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = CarbonGray
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusChange(null) },
                    label = { Text("All") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatus == "active",
                    onClick = { onStatusChange("active") },
                    label = { Text("Active") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatus == "inactive",
                    onClick = { onStatusChange("inactive") },
                    label = { Text("Inactive") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.PauseCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatus == "completed",
                    onClick = { onStatusChange("completed") },
                    label = { Text("Completed") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
fun ProjectCard(
    project: ProjectResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    val statusColor = when (project.status) {
        "active" -> SuccessGreen
        "completed" -> EnergyBlue
        "inactive" -> WarningOrange
        else -> CarbonGray
    }

    val statusIcon = when (project.status) {
        "active" -> Icons.Default.PlayCircle
        "completed" -> Icons.Default.CheckCircle
        "inactive" -> Icons.Default.PauseCircle
        else -> Icons.Default.Help
    }

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
                            .background(statusColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = statusColor
                        )
                    }
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                        Text(
                            text = project.status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Toggle Button
                    IconButton(
                        onClick = {
                            val newStatus = when (project.status) {
                                "active" -> "inactive"
                                "inactive" -> "active"
                                else -> "active"
                            }
                            onStatusUpdate(newStatus)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (project.status == "active") Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (project.status == "active") "Pause project" else "Resume project",
                            modifier = Modifier.size(18.dp),
                            tint = statusColor
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit project",
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
                            contentDescription = "Delete project",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFE57373)
                        )
                    }
                }
            }

            // Description
            if (!project.description.isNullOrBlank()) {
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            // Location and crop info
            project.location?.let { location ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (location.crop_type != null) {
                        ProjectInfoItem(
                            label = "Crop Type",
                            value = location.crop_type.replaceFirstChar { it.uppercase() },
                            icon = Icons.Default.Grass,
                            color = ClimateGreen
                        )
                    }
                    if (location.expected_yield != null) {
                        ProjectInfoItem(
                            label = "Expected Yield",
                            value = location.expected_yield,
                            icon = Icons.Default.Agriculture,
                            color = SolarYellow
                        )
                    }
                }
            }

            // Creation date
            Text(
                text = "Created: ${formatDate(project.created_at?:"")}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF888888)
            )
        }
    }
}

@Composable
fun ProjectInfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Column {
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
}

@Composable
fun ProjectsEmptyState(
    onCreateProject: () -> Unit
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
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ClimateGreen
                )
            }
            Text(
                text = "No Projects Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = "Start your carbon farming journey by creating your first project",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onCreateProject,
                colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Project")
            }
        }
    }
}

@Composable
fun ProjectsEmptySearchState(
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
                text = "No projects found matching \"$searchQuery\"",
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
fun ProjectsLoadingContent() {
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
                text = "Loading projects...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun ProjectsErrorContent(
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

// Helper function to format date
private fun formatDate(dateString: String): String {
    return try {
        // Simple date formatting - you might want to use a proper date library
        val parts = dateString.split("T")[0].split("-")
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        "Unknown"
    }
}

// Dialog components would go here - CreateProjectDialog, UpdateProjectDialog, DeleteProjectDialog
// These would be similar to the farm dialogs but adapted for project fields