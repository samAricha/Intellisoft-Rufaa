package com.teka.rufaa.modules.patient_registration

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
import androidx.compose.ui.text.input.KeyboardType
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
fun PatientRegistrationScreen(
    navigator: NavHostController,
    viewModel: PatientRegistrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var unique by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var regDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showRegDatePicker by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }

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

    // Handle successful registration
    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            // Clear form
            firstname = ""
            lastname = ""
            unique = ""
            dob = ""
            gender = ""
            regDate = LocalDate.now().toString()
            
            viewModel.resetRegistrationSuccess()
            
            // Optional: Navigate back after a delay
            // kotlinx.coroutines.delay(1500)
            // navigator.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { CustomSnackbarHost(hostState = snackbarManager.hostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Register Patient",
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
            // Header Card
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
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "New Patient Registration",
                            fontFamily = rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Fill in the patient's information",
                            fontFamily = quicksand,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Form Card
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
                    // First Name
                    CustomTextField(
                        value = firstname,
                        onValueChange = { 
                            firstname = it
                            viewModel.clearValidationError("firstname")
                        },
                        label = "First Name",
                        placeholder = "Enter first name",
                        leadingIcon = Icons.Default.Person,
                        errorMessage = uiState.validationErrors["firstname"],
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Last Name
                    CustomTextField(
                        value = lastname,
                        onValueChange = { 
                            lastname = it
                            viewModel.clearValidationError("lastname")
                        },
                        label = "Last Name",
                        placeholder = "Enter last name",
                        leadingIcon = Icons.Default.Person,
                        errorMessage = uiState.validationErrors["lastname"],
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Patient ID (Unique)
                    CustomTextField(
                        value = unique,
                        onValueChange = { 
                            unique = it
                            viewModel.clearValidationError("unique")
                        },
                        label = "Patient ID",
                        placeholder = "Enter unique patient ID",
                        leadingIcon = Icons.Default.Badge,
                        errorMessage = uiState.validationErrors["unique"],
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Date of Birth
                    CustomTextField(
                        value = if (dob.isNotEmpty()) formatDateDisplay(dob) else "",
                        onValueChange = { },
                        label = "Date of Birth",
                        placeholder = "Select date of birth",
                        leadingIcon = Icons.Default.Cake,
                        trailingIcon = Icons.Default.CalendarToday,
                        errorMessage = uiState.validationErrors["dob"],
                        readOnly = true,
                        onClick = { showDatePicker = true }
                    )

                    // Gender
                    CustomTextField(
                        value = gender,
                        onValueChange = { },
                        label = "Gender",
                        placeholder = "Select gender",
                        leadingIcon = Icons.Default.Wc,
                        trailingIcon = Icons.Default.ArrowDropDown,
                        errorMessage = uiState.validationErrors["gender"],
                        readOnly = true,
                        onClick = { showGenderMenu = true }
                    )

                    // Registration Date
                    CustomTextField(
                        value = formatDateDisplay(regDate),
                        onValueChange = { },
                        label = "Registration Date",
                        placeholder = "Select registration date",
                        leadingIcon = Icons.Default.EventAvailable,
                        trailingIcon = Icons.Default.CalendarToday,
                        errorMessage = uiState.validationErrors["reg_date"],
                        readOnly = true,
                        onClick = { showRegDatePicker = true }
                    )

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.registerPatient(
                                firstname = firstname,
                                lastname = lastname,
                                unique = unique,
                                dob = dob,
                                gender = gender,
                                regDate = regDate
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
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Register Patient",
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

    // Date Picker Dialog for DOB
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                dob = date
                viewModel.clearValidationError("dob")
                showDatePicker = false
            },
            title = "Select Date of Birth"
        )
    }

    // Date Picker Dialog for Registration Date
    if (showRegDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showRegDatePicker = false },
            onDateSelected = { date ->
                regDate = date
                viewModel.clearValidationError("reg_date")
                showRegDatePicker = false
            },
            title = "Select Registration Date"
        )
    }

    // Gender Dropdown Menu
    if (showGenderMenu) {
        AlertDialog(
            onDismissRequest = { showGenderMenu = false },
            title = {
                Text(
                    text = "Select Gender",
                    fontFamily = rajdhani,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Male", "Female", "Other").forEach { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                gender = option
                                viewModel.clearValidationError("gender")
                                showGenderMenu = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (gender == option) Color(0xFF006A72).copy(alpha = 0.1f)
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
                                    selected = gender == option,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF006A72)
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
                TextButton(onClick = { showGenderMenu = false }) {
                    Text(
                        text = "Cancel",
                        fontFamily = quicksand,
                        color = Color(0xFF006A72)
                    )
                }
            }
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