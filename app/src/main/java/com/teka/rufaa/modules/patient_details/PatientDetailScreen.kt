package com.teka.rufaa.modules.patient_details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.teka.rufaa.modules.patients_list.calculateAge
import com.teka.rufaa.modules.patients_list.formatDate
import com.teka.rufaa.ui.theme.quicksand
import com.teka.rufaa.ui.theme.rajdhani
import com.teka.rufaa.utils.ui_components.CustomSnackbarHost
import com.teka.rufaa.utils.ui_components.HandleSnackbarMessages
import com.teka.rufaa.utils.ui_components.SnackbarManagerWithEncoding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Int,
    navigator: NavHostController,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val hostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val snackbarManager = remember {
        SnackbarManagerWithEncoding(
            hostState = hostState,
            haptic = haptic
        )
    }

    HandleSnackbarMessages(
        snackbarManager = snackbarManager,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onClearError = { viewModel.clearError() },
        onClearSuccess = { viewModel.clearSuccess() }
    )

    LaunchedEffect(patientId) {
        viewModel.loadPatientDetail(patientId)
    }

    Scaffold(
        snackbarHost = { CustomSnackbarHost(hostState = snackbarManager.hostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patient Details",
                        fontFamily = rajdhani,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF006A72),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF006A72)
                        )
                    }
                }
                uiState.patient != null -> {
                    PatientDetailContent(
                        patient = uiState.patient!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFEF4444)
                            )
                            Text(
                                text = "Failed to load patient details",
                                fontFamily = quicksand,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280)
                            )
                            Button(
                                onClick = { viewModel.loadPatientDetail(patientId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006A72)
                                )
                            ) {
                                Text(
                                    text = "Retry",
                                    fontFamily = quicksand,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientDetailContent(
    patient: com.teka.rufaa.modules.patients_list.PatientDto,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Patient Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (patient.gender == "Male") Color(0xFFDCFCE7)
                            else Color(0xFFFCE7F3)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${patient.firstname.firstOrNull()?.uppercaseChar() ?: ""}${patient.lastname.firstOrNull()?.uppercaseChar() ?: ""}",
                        fontFamily = rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = if (patient.gender == "Male") Color(0xFF059669)
                        else Color(0xFFDB2777)
                    )
                }

                // Name
                Text(
                    text = "${patient.firstname} ${patient.lastname}",
                    fontFamily = rajdhani,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color(0xFF1A1A1A)
                )

                // Gender Badge
                Surface(
                    color = if (patient.gender == "Male") Color(0xFFDCFCE7)
                    else Color(0xFFFCE7F3),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = patient.gender,
                        fontFamily = quicksand,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (patient.gender == "Male") Color(0xFF059669)
                        else Color(0xFFDB2777),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Quick Info Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickInfoCard(
                icon = Icons.Default.DateRange,
                label = "Age",
                value = calculateAge(patient.dob),
                modifier = Modifier.weight(1f)
            )
            QuickInfoCard(
                icon = Icons.Default.AccountBox,
                label = "Patient ID",
                value = patient.unique,
                modifier = Modifier.weight(1f)
            )
        }

        // Personal Information Section
        SectionCard(
            title = "Personal Information",
            icon = Icons.Default.Person
        ) {
            InfoRow(
                label = "First Name",
                value = patient.firstname
            )
            Divider(color = Color(0xFFE5E7EB))
            InfoRow(
                label = "Last Name",
                value = patient.lastname
            )
            Divider(color = Color(0xFFE5E7EB))
            InfoRow(
                label = "Date of Birth",
                value = formatDate(patient.dob)
            )
            Divider(color = Color(0xFFE5E7EB))
            InfoRow(
                label = "Gender",
                value = patient.gender
            )
        }

        // Registration Information Section
        SectionCard(
            title = "Registration Information",
            icon = Icons.Default.Info
        ) {
            InfoRow(
                label = "Registration Date",
                value = formatDate(patient.reg_date)
            )
            Divider(color = Color(0xFFE5E7EB))
            InfoRow(
                label = "User ID",
                value = patient.user_id.toString()
            )
            Divider(color = Color(0xFFE5E7EB))
            InfoRow(
                label = "Created At",
                value = formatDateTime(patient.created_at)
            )
            Divider(color = Color(0xFFE5E7EB))
            InfoRow(
                label = "Last Updated",
                value = formatDateTime(patient.updated_at)
            )
        }
    }
}

@Composable
fun QuickInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF006A72)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontFamily = quicksand,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                fontFamily = rajdhani,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF006A72),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontFamily = rajdhani,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A)
                )
            }
            content()
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = quicksand,
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = quicksand,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.weight(1f)
        )
    }
}

fun formatDateTime(dateTimeString: String): String {
    return try {
        val dateTime = LocalDateTime.parse(dateTimeString.replace("Z", ""))
        dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"))
    } catch (e: Exception) {
        dateTimeString
    }
}