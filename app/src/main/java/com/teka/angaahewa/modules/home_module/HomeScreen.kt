package com.teka.angaahewa.modules.home_module

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.teka.angaahewa.core.navigation.AppScreens
import com.teka.angaahewa.utils.ui_components.CustomDialog
import timber.log.Timber
import com.teka.angaahewa.ui.theme.CarbonGray
import com.teka.angaahewa.ui.theme.ClimateGreen
import com.teka.angaahewa.ui.theme.EnergyBlue
import com.teka.angaahewa.ui.theme.SkyBlue
import com.teka.angaahewa.ui.theme.SolarYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    val showLogoutDialog =  uiState.showLogoutDialog

    if(showLogoutDialog) {
        Timber.tag("HomePage").i("showLogoutDialog: $showLogoutDialog")
        CustomDialog(
            value = "",
            setShowDialog = {
                viewModel.updateUiState { copy(showLogoutDialog = it) }
            },
            setValue = {
                Timber.tag("HomePage").i("HomePage : %s", it)
            }
        )
    }






    Scaffold(
        topBar = {
            ClimateTopBar(viewModel)
        },
        floatingActionButton = {
            ClimateActionButtons(navController = navController)
        },
        containerColor = Color(0xFFF8FFFE)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> ClimateLoadingContent()
                uiState.error != null -> ClimateErrorContent(uiState.error!!)
                else -> ClimateDashboardContent(
                    profile = uiState.profile,
                    farms = uiState.farms,
                    projects = uiState.projects,
                    navController = navController,
                    homeViewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateTopBar(viewModel: HomeViewModel) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = ClimateGreen,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        "Angaa Hewa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ClimateGreen
                    )
                    Text(
                        "Climate Action Dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = {
                viewModel.updateUiState { copy(showLogoutDialog = true) }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = CarbonGray
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun ClimateActionButtons(navController: NavController) {
    val context = LocalContext.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        FloatingActionButton(
            onClick = {
                navController.navigate(AppScreens.GeotagImageScreen.route)

//                val intent = Intent(Intent.ACTION_DIAL).apply {
//                    data = Uri.parse("tel:+254726741054")
//                }
//                context.startActivity(intent)
            },
            containerColor = EnergyBlue,
            contentColor = Color.White,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Call Support",
                modifier = Modifier.size(20.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                navController.navigate(AppScreens.ChatScreen.route)
            },
            containerColor = ClimateGreen,
            contentColor = Color.White,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Chat Assistant",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Add this to your HomeScreen.kt file - replace the existing ClimateDashboardContent function

@Composable
fun ClimateDashboardContent(
    profile: ProfileResponse?,
    farms: List<FarmResponse>,
    projects: List<ProjectResponse>,
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    var showCreditModal by remember { mutableStateOf(false) }
    var selectedFarm by remember { mutableStateOf<FarmResponse?>(null) }
    var selectedCreditValue by remember { mutableStateOf<CarbonCreditValue?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Climate Hero Section
        item {
            ClimateHeroSection(
                profile = profile,
                userName = homeViewModel.getUserDisplayName() ?: "Climate Champion"
            )
        }

        // Quick Navigation Section
        item {
            QuickNavigationSection(navController)
        }

        // Carbon Impact Stats
        item {
            CarbonImpactSection(
                farmCount = farms.size,
                projectCount = projects.size,
                activeProjects = projects.count { it.status == "active" }
            )
        }

        // Carbon Sites Section with View All button
        if (farms.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClimateSectionHeader("Carbon Monitoring Sites", Icons.Default.Forest)
                    TextButton(
                        onClick = { navController.navigate(AppScreens.FarmsScreen.route) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ClimateGreen)
                    ) {
                        Text("View All")
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(farms.take(3)) { farm -> // Show only first 3 farms
                        CarbonSiteCard(
                            farm = farm,
                            onCardClick = { clickedFarm, creditValue ->
                                selectedFarm = clickedFarm
                                selectedCreditValue = creditValue
                                showCreditModal = true
                            }
                        )
                    }
                    if (farms.size > 3) {
                        item {
                            ViewMoreCard(
                                title = "View All Sites",
                                subtitle = "${farms.size} total sites",
                                onClick = { navController.navigate(AppScreens.FarmsScreen.route) }
                            )
                        }
                    }
                }
            }
        }

        // Energy Projects Section with View All button
        if (projects.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClimateSectionHeader("Current Projects", Icons.Default.ElectricBolt)
                    TextButton(
                        onClick = { navController.navigate(AppScreens.ProjectsScreen.route) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ClimateGreen)
                    ) {
                        Text("View All")
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            // Show only first 2 projects
            items(projects.take(2)) { project ->
                EnergyProjectCard(project)
            }
            if (projects.size > 2) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(AppScreens.ProjectsScreen.route) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = ClimateGreen.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "View ${projects.size - 2} more projects",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ClimateGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Empty state with navigation options
        if (farms.isEmpty() && projects.isEmpty()) {
            item {
                ClimateEmptyStateWithNavigation(navController)
            }
        }

        // Extra spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Carbon Credit Modal (existing code remains the same)
    selectedFarm?.let { farm ->
        selectedCreditValue?.let { creditValue ->
            CarbonCreditValueModal(
                isVisible = showCreditModal,
                farm = farm,
                creditValue = creditValue,
                onDismiss = {
                    showCreditModal = false
                    selectedFarm = null
                    selectedCreditValue = null
                }
            )
        }
    }
}

@Composable
fun QuickNavigationSection(navController: NavController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CarbonGray
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Manage Sites",
                subtitle = "Carbon monitoring",
                icon = Icons.Default.Agriculture,
                backgroundColor = ClimateGreen,
                onClick = { navController.navigate(AppScreens.FarmsScreen.route) }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "View Projects",
                subtitle = "Climate solutions",
                icon = Icons.Default.Assignment,
                backgroundColor = EnergyBlue,
                onClick = { navController.navigate(AppScreens.ProjectsScreen.route) }
            )
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ViewMoreCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ClimateGreen.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = ClimateGreen
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ClimateGreen,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ClimateEmptyStateWithNavigation(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ClimateGreen
                )
            }
            Text(
                text = "Start Your Climate Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = "Begin monitoring your carbon impact and implementing renewable energy solutions",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { navController.navigate(AppScreens.FarmsScreen.route) },
                    colors = ButtonDefaults.buttonColors(containerColor = ClimateGreen)
                ) {
                    Icon(
                        Icons.Default.Agriculture,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Site")
                }

                OutlinedButton(
                    onClick = { navController.navigate(AppScreens.ProjectsScreen.route) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EnergyBlue),
                    border = BorderStroke(1.dp, EnergyBlue)
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Project")
                }
            }
        }
    }
}


@Composable
fun ClimateHeroSection(
    profile: ProfileResponse?,
    userName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ClimateGreen.copy(alpha = 0.05f),
                            EnergyBlue.copy(alpha = 0.05f),
                            SkyBlue.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ClimateGreen,
                                    ClimateGreen.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.toString()?.uppercase() ?: "C",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray
                    )
                    Text(
                        text = "Climate Action Partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClimateGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CarbonImpactSection(
    farmCount: Int,
    projectCount: Int,
    activeProjects: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ClimateStatCard(
            modifier = Modifier.weight(1f),
            title = "Monitoring Sites",
            value = farmCount.toString(),
            subtitle = "Carbon sequestration",
            icon = Icons.Default.Park,
            backgroundColor = ClimateGreen
        )
        ClimateStatCard(
            modifier = Modifier.weight(1f),
            title = "Energy Projects",
            value = projectCount.toString(),
            subtitle = "Clean solutions",
            icon = Icons.Default.ElectricBolt,
            backgroundColor = EnergyBlue
        )
        ClimateStatCard(
            modifier = Modifier.weight(1f),
            title = "Active Impact",
            value = activeProjects.toString(),
            subtitle = "Ongoing projects",
            icon = Icons.Default.TrendingUp,
            backgroundColor = SolarYellow
        )
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

// Add this function to calculate available green energy products
fun calculateAffordableProducts(totalValue: Double): List<Pair<GreenEnergyProduct, Int>> {
    val products = listOf(
        GreenEnergyProduct("Solar Panel (300W)", "Monocrystalline solar panel", 150.0, "panel", Icons.Default.WbSunny, "Solar"),
        GreenEnergyProduct("LED Light Bulb (12W)", "Energy efficient LED lighting", 5.0, "bulb", Icons.Default.Lightbulb, "Lighting"),
        GreenEnergyProduct("Solar Water Heater", "100L solar water heating system", 300.0, "system", Icons.Default.WaterDrop, "Water Heating"),
        GreenEnergyProduct("Energy Storage Battery", "12V 100Ah lithium battery", 200.0, "battery", Icons.Default.Battery4Bar, "Storage"),
        GreenEnergyProduct("Solar Charge Controller", "MPPT charge controller 40A", 80.0, "controller", Icons.Default.ElectricBolt, "Control"),
        GreenEnergyProduct("Energy Efficient Fan", "DC ceiling fan 48W", 60.0, "fan", Icons.Default.Air, "Cooling"),
        GreenEnergyProduct("Solar Inverter (1000W)", "Pure sine wave inverter", 120.0, "inverter", Icons.Default.Power, "Power")
    )

    return products.mapNotNull { product ->
        val quantity = (totalValue / product.costPerUnit).toInt()
        if (quantity > 0) Pair(product, quantity) else null
    }.sortedByDescending { it.second }
}

// Modified CarbonSiteCard with click functionality
@Composable
fun CarbonSiteCard(
    farm: FarmResponse,
    onCardClick: (FarmResponse, CarbonCreditValue) -> Unit = { _, _ -> }
) {
    // Calculate carbon credit value
    val acres = farm.location?.acres ?: 0
    val carbonCredits = acres * 2.3 // tons of CO2
    val creditValuePerTon = 15.0 // USD per ton (you can make this dynamic)
    val totalValue = carbonCredits * creditValuePerTon

    val creditValue = CarbonCreditValue(
        totalCredits = carbonCredits,
        creditValuePerTon = creditValuePerTon,
        totalValue = totalValue
    )

    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onCardClick(farm, creditValue) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Forest,
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
                        text = "Monitoring Site",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }

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
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Carbon metrics with click indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${farm.location?.acres ?: 0} acres",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClimateGreen,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Total area",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888888)
                    )
                }
                Column {
                    Text(
                        text = "~${String.format("%.1f", carbonCredits)}t",
                        style = MaterialTheme.typography.bodySmall,
                        color = EnergyBlue,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "CO₂ potential",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888888)
                    )
                }
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Tap for details",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFBBBBBB)
                )
            }
        }
    }
}

@Composable
fun EnergyProjectCard(project: ProjectResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                ClimateStatusChip(project.status)
            }

            // Project type indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        project.name.contains("solar", ignoreCase = true) -> Icons.Default.WbSunny
                        project.name.contains("greenhouse", ignoreCase = true) -> Icons.Default.Grass
                        project.name.contains("irrigation", ignoreCase = true) -> Icons.Default.WaterDrop
                        else -> Icons.Default.ElectricBolt
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = EnergyBlue
                )
                Text(
                    text = when {
                        project.name.contains("solar", ignoreCase = true) -> "Solar Energy"
                        project.name.contains("greenhouse", ignoreCase = true) -> "Sustainable Agriculture"
                        project.name.contains("irrigation", ignoreCase = true) -> "Water Conservation"
                        else -> "Climate Solution"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = EnergyBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ClimateStatusChip(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "active" -> Pair(ClimateGreen, Color.White)
        "completed" -> Pair(EnergyBlue, Color.White)
        "pending" -> Pair(SolarYellow, Color.White)
        else -> Pair(Color(0xFFE0E0E0), CarbonGray)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ClimateEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = ClimateGreen
                )
            }
            Text(
                text = "Start Your Climate Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarbonGray
            )
            Text(
                text = "Begin monitoring your carbon impact and implementing renewable energy solutions",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ClimateLoadingContent() {
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
                text = "Loading angaa data...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun ClimateErrorContent(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}



// Carbon Credit Value Modal
@Composable
fun CarbonCreditValueModal(
    isVisible: Boolean,
    farm: FarmResponse,
    creditValue: CarbonCreditValue,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = ClimateGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Carbon Credit Value",
                        style = MaterialTheme.typography.titleLarge,
                        color = CarbonGray
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Farm info
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ClimateGreen.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = farm.profiles?.name ?: "Carbon Site",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonGray
                                )
                                Text(
                                    text = "${farm.location?.acres ?: 0} acres • ${String.format("%.1f", creditValue.totalCredits)} tons CO₂",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    text = "Total Value: KSH ${String.format("%.2f", creditValue.totalValue)} USD",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ClimateGreen
                                )
                            }
                        }
                    }

                    // Available green energy products
                    item {
                        Text(
                            text = "Available Green Energy Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray
                        )
                    }

                    val affordableProducts = calculateAffordableProducts(creditValue.totalValue)
                    items(affordableProducts.take(6)) { (product, quantity) ->
                        GreenEnergyProductItem(product, quantity)
                    }

                    if (affordableProducts.isEmpty()) {
                        item {
                            Text(
                                text = "Build more carbon credits to unlock green energy products!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = ClimateGreen)
                ) {
                    Text("Close")
                }
            }
        )
    }
}



@Composable
fun GreenEnergyProductItem(
    product: GreenEnergyProduct,
    quantity: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EnergyBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = product.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = EnergyBlue
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = CarbonGray
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "KSH ${product.costPerUnit} per ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF888888)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ClimateGreen
                )
                Text(
                    text = product.unit + "s",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF888888)
                )
            }
        }
    }
}