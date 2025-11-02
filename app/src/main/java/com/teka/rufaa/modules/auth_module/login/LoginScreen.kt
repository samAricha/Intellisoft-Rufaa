package com.teka.rufaa.modules.auth_module.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
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
import com.teka.rufaa.ui.theme.quicksand
import com.teka.rufaa.ui.theme.rajdhani
import com.teka.rufaa.utils.composition_locals.LocalDialogController
import com.teka.rufaa.utils.ui_components.CustomDropDown3
import com.teka.rufaa.utils.ui_components.CustomInputTextField2
import com.teka.rufaa.utils.ui_components.CustomSnackbarHost
import com.teka.rufaa.utils.ui_components.HandleSnackbarMessages
import com.teka.rufaa.utils.ui_components.InputDialogWidget
import com.teka.rufaa.utils.ui_components.SnackbarManagerWithEncoding
import timber.log.Timber
import com.teka.rufaa.R

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

    val roleOptions = listOf("Admin", "CM", "Agent", "Loader", "Field Officer")
    var isPasswordOpen by remember { mutableStateOf(false) }

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
                viewModel.changeBaseUrl(inputUrl)
                dialogController.triggerDialog(false)
            },
            onDismiss = {
                dialogController.triggerDialog(false)
            }
        )
    }



    //screen validators
    val mobileValidator: (String) -> String? = { value ->
        when {
            value.isBlank() -> "Mobile number is required"
            value.length < 10 -> "Mobile number must be at least 10 digits"
            !value.all { it.isDigit() } -> "Mobile number must contain only digits"
            else -> {
                viewModel.clearFieldError("mobile")
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

    val userRoleValidator: (String) -> String? = { value ->
        if (value.isBlank()) "Please select a role" else {
            viewModel.clearFieldError("role")
            null
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
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                        painter = painterResource(id = R.drawable.rufaa_logo),
                        contentDescription = "Rufaa Logo",
                        modifier = Modifier.size(110.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = rajdhani
            )

            Text(
                text = "Sign in to continue to Rufaa",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6B7280),
                fontFamily = rajdhani,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(38.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    CustomDropDown3(
                        labelText = "Select Role",
                        options = roleOptions,
                        selectedValue = uiState.role?:"",
                        onValueChange = {it -> viewModel.updateUiState { copy(role = it) } },
                        onOptionSelected = {it -> viewModel.updateUiState { copy(role = it) }},//this can be empty i.e {}, used both of these two bcs of complex objects support
                        onValidate = userRoleValidator,
                        optionTextProvider = { option ->
                            Text(
                                text = option,
                                fontFamily = quicksand,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        isOptional = false,
                        enableRealTimeValidation = true,
                        errorMessage = fieldErrors["role"]
                    )


                    CustomInputTextField2(
                        labelText = "Mobile Number",
                        value = uiState.mobile?:"",
                        onValueChange = { viewModel.updateUiState { copy(mobile = it) } },
                        onValidate = mobileValidator,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = Color(0xFF6B7280)
                            )
                        },
                        errorMessage = fieldErrors["mobile"],
                    )

                    CustomInputTextField2(
                        labelText = "Password",
                        value = uiState.password?:"",
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.validateAndSubmitSimple(
                                uiState.mobile to mobileValidator,
                                uiState.password to passwordValidator,
                                uiState.role to userRoleValidator
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
                text = "Simplify Patient Registration & Health Tracking",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF006A72),
                fontFamily = quicksand,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Smart • Reliable • Compassionate Care",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6B7280),
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