package com.teka.rufaa.modules.vitals.general_assesment

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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun GeneralAssessmentScreen(
    navigator: NavHostController,
    patientId: String,
    viewModel: GeneralAssessmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var visitDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var generalHealth by remember { mutableStateOf("") }
    var onDiet by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showHealthMenu by remember { mutableStateOf(false) }
    var showDietMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
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

    // Handle successful assessment submission
    LaunchedEffect(uiState.assessmentSuccess) {
        if (uiState.assessmentSuccess) {
            viewModel.resetAssessmentSuccess()
            // Navigate to patient listing
            navigator.navigate("patient_listing") {
                popUpTo("patient_listing") { inclusive = true }
            }
        }
    }

    Scaffold(
        snackbarHost = { CustomSnackbarHost(hostState = snackbarManager.hostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "General Assessment",
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
                    containerColor = Color(0xFF10B981),
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
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981)),
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
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "General Health Assessment",
                            fontFamily = rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Patient ID: $patientId | Form A",
                            fontFamily = quicksand,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
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
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "This form is for patients with BMI ≤ 25",
                        fontFamily = quicksand,
                        fontSize = 12.sp,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }

            // Assessment Form Card
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

                    // General Health
                    CustomTextField(
                        value = generalHealth,
                        onValueChange = { },
                        label = "General Health",
                        placeholder = "Select health status",
                        leadingIcon = Icons.Default.Favorite,
                        trailingIcon = Icons.Default.ArrowDropDown,
                        errorMessage = uiState.validationErrors["general_health"],
                        readOnly = true,
                        onClick = { showHealthMenu = true }
                    )

                    // On Diet to Lose Weight
                    CustomTextField(
                        value = onDiet,
                        onValueChange = { },
                        label = "Have you ever been on a diet to lose weight?",
                        placeholder = "Select Yes or No",
                        leadingIcon = Icons.Default.Restaurant,
                        trailingIcon = Icons.Default.ArrowDropDown,
                        errorMessage = uiState.validationErrors["on_diet_to_lose_weight"],
                        readOnly = true,
                        onClick = { showDietMenu = true }
                    )

                    // Comments
                    CustomTextField(
                        value = comments,
                        onValueChange = { 
                            comments = it
                            viewModel.clearValidationError("comments")
                        },
                        label = "Comments",
                        placeholder = "Enter any additional comments",
                        leadingIcon = Icons.Default.Notes,
                        errorMessage = uiState.validationErrors["comments"],
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = false,
                        maxLines = 4
                    )

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submitGeneralAssessment(
                                visitDate = visitDate,
                                generalHealth = generalHealth,
                                onDietToLoseWeight = onDiet,
                                comments = comments,
                                patientId = patientId
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
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
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Submit Assessment",
                                    fontFamily = rajdhani,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
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

    // General Health Dropdown
    if (showHealthMenu) {
        OptionDialog(
            title = "Select General Health",
            options = listOf("Good", "Poor"),
            currentSelection = generalHealth,
            onOptionSelected = { option ->
                generalHealth = option
                viewModel.clearValidationError("general_health")
                showHealthMenu = false
            },
            onDismiss = { showHealthMenu = false }
        )
    }

    // Diet Dropdown
    if (showDietMenu) {
        OptionDialog(
            title = "Diet to Lose Weight",
            options = listOf("Yes", "No"),
            currentSelection = onDiet,
            onOptionSelected = { option ->
                onDiet = option
                viewModel.clearValidationError("on_diet_to_lose_weight")
                showDietMenu = false
            },
            onDismiss = { showDietMenu = false }
        )
    }
}

@Composable
fun OptionDialog(
    title: String,
    options: List<String>,
    currentSelection: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = rajdhani,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOptionSelected(option) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentSelection == option) 
                                Color(0xFF10B981).copy(alpha = 0.1f)
                            else Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSelection == option,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF10B981)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option,
                                fontFamily = quicksand,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontFamily = quicksand,
                    color = Color(0xFF10B981)
                )
            }
        }
    )
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1
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
            trailingIcon = trailingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                }
            },
            isError = errorMessage != null,
            readOnly = readOnly,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                errorBorderColor = Color(0xFFEF4444),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = singleLine,
            maxLines = maxLines,
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
                    color = Color(0xFF10B981)
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
                selectedDayContainerColor = Color(0xFF10B981),
                todayContentColor = Color(0xFF10B981),
                todayDateBorderColor = Color(0xFF10B981)
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