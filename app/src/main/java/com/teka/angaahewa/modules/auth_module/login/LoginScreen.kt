package com.teka.angaahewa.modules.auth_module.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.teka.angaahewa.ui.theme.quicksand
import com.teka.angaahewa.ui.theme.rajdhani
import com.teka.angaahewa.utils.ui_components.CustomInputTextField2
import com.teka.angaahewa.utils.ui_components.CustomSnackbarHost
import com.teka.angaahewa.utils.ui_components.HandleSnackbarMessages
import com.teka.angaahewa.utils.ui_components.InputDialogWidget
import com.teka.angaahewa.utils.ui_components.SnackbarManagerWithEncoding
import com.teka.angaahewa.utils.composition_locals.LocalDialogController
import timber.log.Timber
import com.teka.angaahewa.R


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navigator: NavHostController,
    viewModel: LoginScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.loginFormUiState.collectAsState()
    val fieldErrors by viewModel.fieldErrors.collectAsState()

    val baseUrl = viewModel.baseUrl.collectAsState()
    val loginState = viewModel.loginState.value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dialogController = LocalDialogController.current
    val scrollState = rememberScrollState()

    var isPasswordOpen by remember { mutableStateOf(false) }
    var isDemoMenuExpanded by remember { mutableStateOf(false) }

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

    if (dialogController.showDialog.value) {
        InputDialogWidget(
            title = "Change link",
            label = "enter link: ",
            initialValue = baseUrl.value,
            onSubmit = { inputUrl ->
                Timber.d("User input: $inputUrl")
                dialogController.triggerDialog(false)
            },
            onDismiss = {
                dialogController.triggerDialog(false)
            }
        )
    }

    //screen validators
    val emailValidator: (String) -> String? = { value ->
        when {
            value.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() -> "Please enter a valid email address"
            else -> {
                viewModel.clearFieldError("email")
                null
            }
        }
    }
    val passwordValidator: (String) -> String? = { value ->
        when {
            value.isBlank() -> "Password is required"
            value.length < 4 -> "Password must be at least 4 characters"
            else -> {
                viewModel.clearFieldError("password")
                null
            }
        }
    }

    Scaffold(
        snackbarHost = { CustomSnackbarHost(hostState = snackbarManager.hostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { scaffoldPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(scaffoldPadding)
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.anga_hewa_logo),
                        contentDescription = "AngaaHewa Logo",
                        modifier = Modifier.size(110.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Welcome Back",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        fontFamily = rajdhani
                    )

                    Text(
                        text = "Sign in to continue to Angaa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF6B7280),
                        fontFamily = rajdhani,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Demo Credentials Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isDemoMenuExpanded,
                        onExpandedChange = { isDemoMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Select Demo Credentials",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Quick Login", fontFamily = quicksand) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color(0xFF6B7280)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isDemoMenuExpanded,
                            onDismissRequest = { isDemoMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            "Baite Account",
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = rajdhani,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            "baite.murume@gmail.com",
                                            fontSize = 13.sp,
                                            color = Color(0xFF6B7280),
                                            fontFamily = quicksand
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.updateUiState {
                                        copy(
                                            email = "baite.murume@gmail.com",
                                            password = "baite123"
                                        )
                                    }
                                    isDemoMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            "Demo Account",
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = rajdhani,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            "demo@angaa.com",
                                            fontSize = 13.sp,
                                            color = Color(0xFF6B7280),
                                            fontFamily = quicksand
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.updateUiState {
                                        copy(
                                            email = "demo@angaa.com",
                                            password = "demo123"
                                        )
                                    }
                                    isDemoMenuExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    CustomInputTextField2(
                        labelText = "Email",
                        value = uiState.email ?: "",
                        onValueChange = { viewModel.updateUiState { copy(email = it) } },
                        onValidate = emailValidator,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = "Email",
                                tint = Color(0xFF6B7280)
                            )
                        },
                        errorMessage = fieldErrors["email"],
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CustomInputTextField2(
                        labelText = "Password",
                        value = uiState.password ?: "",
                        onValueChange = { viewModel.updateUiState { copy(password = it) } },
                        onValidate = passwordValidator,
                        visualTransformation = if (isPasswordOpen) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    isPasswordOpen = !isPasswordOpen
                                }
                            ) {
                                val icon = if (isPasswordOpen) R.drawable.ic_eye_close else R.drawable.ic_eye_open
                                Icon(
                                    painter = painterResource(id = icon),
                                    contentDescription = if (isPasswordOpen) "Hide Password" else "Show Password",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        errorMessage = fieldErrors["password"],
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            viewModel.validateAndSubmitSimple(
                                uiState.email to emailValidator,
                                uiState.password to passwordValidator,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (loginState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = rajdhani
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Streamline Your Monitoring Operations",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280),
                fontFamily = quicksand,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Efficient • Organized • Productive",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF9CA3AF),
                fontFamily = quicksand,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (loginState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Signing in...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280),
                                fontFamily = quicksand
                            )
                        }
                    }
                }
            }
        }
    }
}