package com.teka.rufaa.modules.patients_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.teka.rufaa.ui.theme.quicksand
import com.teka.rufaa.ui.theme.rajdhani
import com.teka.rufaa.utils.ui_components.CustomSnackbarHost
import com.teka.rufaa.utils.ui_components.HandleSnackbarMessages
import com.teka.rufaa.utils.ui_components.SnackbarManagerWithEncoding
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsListScreen(
    navigator: NavHostController,
    viewModel: PatientsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val filteredPatients by viewModel.filteredPatients.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

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

    LaunchedEffect(Unit) {
        viewModel.loadPatients()
    }

    Scaffold(
        snackbarHost = { CustomSnackbarHost(hostState = snackbarManager.hostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patients",
                        fontFamily = rajdhani,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF006A72),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigator.navigate("patient_registration") },
                containerColor = Color(0xFF006A72)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Patient",
                    tint = Color.White
                )
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchPatients(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = {
                        Text(
                            text = "Search patients...",
                            fontFamily = quicksand,
                            color = Color(0xFF9CA3AF)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF6B7280)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF006A72),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // Patients Count
            Text(
                text = "${filteredPatients.size} Patient(s)",
                fontFamily = quicksand,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Loading State
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF006A72)
                    )
                }
            }
            // Empty State
            else if (filteredPatients.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF9CA3AF)
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "No patients found" else "No matching patients",
                            fontFamily = quicksand,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        )
                        if (searchQuery.isEmpty()) {
                            Button(
                                onClick = { navigator.navigate("patient_registration") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006A72)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Register New Patient",
                                    fontFamily = quicksand,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            // Patients List
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPatients) { patient ->
                        PatientCard(
                            patient = patient,
                            onViewDetails = {
                                navigator.navigate("patient_detail/${patient.id}")
                            },
                            onRecordVitals = {
                                navigator.navigate("vitals/${patient.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatientCard(
    patient: PatientDto,
    onViewDetails: () -> Unit,
    onRecordVitals: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main Patient Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewDetails() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar and Info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
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
                            fontSize = 18.sp,
                            color = if (patient.gender == "Male") Color(0xFF059669)
                            else Color(0xFFDB2777)
                        )
                    }

                    // Patient Info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${patient.firstname} ${patient.lastname}",
                            fontFamily = rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1A1A1A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ID: ${patient.unique}",
                                fontFamily = quicksand,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = "•",
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = calculateAge(patient.dob),
                                fontFamily = quicksand,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        // Gender Badge
                        Surface(
                            color = if (patient.gender == "Male") Color(0xFFDCFCE7)
                            else Color(0xFFFCE7F3),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = patient.gender,
                                fontFamily = quicksand,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (patient.gender == "Male") Color(0xFF059669)
                                else Color(0xFFDB2777),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Registration Date
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Registered",
                        fontFamily = quicksand,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = formatDate(patient.reg_date),
                        fontFamily = quicksand,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            // Divider
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFF3F4F6)
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Record Vitals Button (Primary Action)
                Button(
                    onClick = onRecordVitals,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF006A72)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Record Vitals",
                        fontFamily = quicksand,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // View Details Button (Secondary Action)
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF006A72)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF006A72)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Details",
                        fontFamily = quicksand,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

fun calculateAge(dob: String): String {
    return try {
        val birthDate = LocalDate.parse(dob)
        val age = Period.between(birthDate, LocalDate.now()).years
        "$age yrs"
    } catch (e: Exception) {
        "N/A"
    }
}

fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}