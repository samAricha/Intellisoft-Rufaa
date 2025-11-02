package com.teka.rufaa.modules.vitals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(
    navigator: NavHostController,
    patientId: String,
    viewModel: VitalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var visitDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val hostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val snackbarManager = remember {
        SnackbarManagerWithEncoding(
            hostState = hostState,
            haptic = haptic
        )
    }

    // Calculate BMI whenever height or weight changes
    LaunchedEffect(height, weight) {
        viewModel.calculateBmi(height, weight)
    }

    HandleSnackbarMessages(
        snackbarManager = snackbarManager,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onClearError = { viewModel.clearError() },
        onClearSuccess = { viewModel.clearSuccess() }
    )

    // Handle successful vitals submission and navigation based on BMI
    LaunchedEffect(uiState.vitalsSuccess) {
        if (uiState.vitalsSuccess) {
            viewModel.resetVitalsSuccess()

            // Navigate based on BMI: <= 25 goes to General, > 25 goes to Overweight
            when (uiState.assessmentRoute) {
                AssessmentRoute.GENERAL -> {
                    // BMI <= 25: Navigate to General Assessment Page
                    navigator.navigate("general_assessment/$patientId")
                }
                AssessmentRoute.OVERWEIGHT -> {
                    // BMI > 25: Navigate to Overweight Assessment Page
                    navigator.navigate("overweight_assessment/$patientId")
                }
                AssessmentRoute.UNKNOWN -> {
                    // Fallback - should not happen
                    navigator.navigate("patient_listing")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { CustomSnackbarHost(hostState = snackbarManager.hostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patient Vitals",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with Patient Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF006A72)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Record Vital Signs",
                            fontFamily = rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Patient ID: $patientId",
                            fontFamily = quicksand,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // BMI Display Card (if calculated)
            if (uiState.calculatedBmi != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = viewModel.getBmiColor().copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Body Mass Index",
                                fontFamily = quicksand,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = String.format("%.1f", uiState.calculatedBmi),
                                fontFamily = rajdhani,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = viewModel.getBmiColor()
                            )
                        }

                        // BMI Category Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = viewModel.getBmiColor()
                        ) {
                            Text(
                                text = viewModel.getBmiCategoryText(),
                                fontFamily = quicksand,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Vitals Form Card
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visit Date
                    CustomTextField(
                        value = formatDateDisplay(visitDate),
                        onValueChange = { },
                        label = "Visit Date",
                        placeholder = "Select visit date",
                        leadingIcon = Icons.Default.CalendarToday,
                        trailingIcon = Icons.Default.Event,
                        errorMessage = uiState.validationErrors["visit_date"],
                        readOnly = true,
                        onClick = { showDatePicker = true }
                    )

                    // Height (in CM)
                    CustomTextField(
                        value = height,
                        onValueChange = {
                            height = it
                            viewModel.clearValidationError("height")
                        },
                        label = "Height (cm)",
                        placeholder = "Enter height in centimeters",
                        leadingIcon = Icons.Default.Height,
                        errorMessage = uiState.validationErrors["height"],
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        trailingContent = {
                            Text(
                                text = "cm",
                                fontFamily = quicksand,
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    )

                    // Weight (in KG)
                    CustomTextField(
                        value = weight,
                        onValueChange = {
                            weight = it
                            viewModel.clearValidationError("weight")
                        },
                        label = "Weight (kg)",
                        placeholder = "Enter weight in kilograms",
                        leadingIcon = Icons.Default.MonitorWeight,
                        errorMessage = uiState.validationErrors["weight"],
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        trailingContent = {
                            Text(
                                text = "kg",
                                fontFamily = quicksand,
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    )

                    // BMI Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3F4F6)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "BMI is automatically calculated: BMI = Weight(kg) / Height(m)²",
                                fontFamily = quicksand,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submitVitals(
                                visitDate = visitDate,
                                height = height,
                                weight = weight,
                                patientId = patientId
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF006A72)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Save Vitals",
                                    fontFamily = rajdhani,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // BMI Reference Guide
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
                    Text(
                        text = "BMI Reference Guide",
                        fontFamily = rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A1A)
                    )

                    BmiReferenceItem(
                        category = "Underweight",
                        range = "< 18.5",
                        color = Color(0xFFFBBF24)
                    )
                    BmiReferenceItem(
                        category = "Normal",
                        range = "18.5 - 24.9",
                        color = Color(0xFF10B981)
                    )
                    BmiReferenceItem(
                        category = "Overweight",
                        range = "≥ 25",
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }

    // Date Picker Dialog for Visit Date
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                visitDate = date
                viewModel.clearValidationError("visit_date")
                showDatePicker = false
            },
            title = "Select Visit Date"
        )
    }
}

@Composable
fun BmiReferenceItem(
    category: String,
    range: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = RoundedCornerShape(2.dp))
            )
            Text(
                text = category,
                fontFamily = quicksand,
                fontSize = 14.sp,
                color = Color(0xFF1A1A1A)
            )
        }
        Text(
            text = range,
            fontFamily = quicksand,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontFamily = quicksand,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color(0xFF1A1A1A)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = quicksand,
                    color = Color(0xFF9CA3AF)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (errorMessage != null) Color(0xFFEF4444) else Color(0xFF6B7280)
                )
            },
            trailingIcon = if (trailingIcon != null) {
                {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                }
            } else if (trailingContent != null) {
                trailingContent
            } else null,
            isError = errorMessage != null,
            readOnly = readOnly,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF006A72),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                errorBorderColor = Color(0xFFEF4444),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                .also { interactionSource ->
                    if (onClick != null) {
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    onClick()
                                }
                            }
                        }
                    }
                }
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontFamily = quicksand,
                fontSize = 12.sp,
                color = Color(0xFFEF4444)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (String) -> Unit,
    title: String
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date.toString())
                    }
                }
            ) {
                Text(
                    text = "OK",
                    fontFamily = quicksand,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF006A72)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Cancel",
                    fontFamily = quicksand,
                    color = Color(0xFF6B7280)
                )
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = title,
                    fontFamily = rajdhani,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            },
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = Color(0xFF006A72),
                todayContentColor = Color(0xFF006A72),
                todayDateBorderColor = Color(0xFF006A72)
            )
        )
    }
}

fun formatDateDisplay(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}