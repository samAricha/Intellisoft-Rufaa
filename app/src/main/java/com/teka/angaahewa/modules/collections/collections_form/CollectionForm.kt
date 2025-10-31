package com.teka.angaahewa.modules.collections.collections_form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.teka.angaahewa.ui.theme.CustomShapes
import com.teka.angaahewa.ui.theme.PrimaryGreen
import com.teka.angaahewa.ui.theme.TextSizeXLarge
import com.teka.angaahewa.utils.bottom_sheet.BottomSheetSelection
import com.teka.angaahewa.utils.bottom_sheet.BottomSheetTextField
import com.teka.angaahewa.utils.formattedTimeBasedOnTimeFormat
import com.teka.angaahewa.utils.ui_components.CustomButton
import com.teka.angaahewa.utils.ui_components.CustomDateBoxField
import com.teka.angaahewa.utils.ui_components.CustomInputTextField2
import com.teka.angaahewa.utils.ui_components.CustomSnackbarHost
import com.teka.angaahewa.utils.ui_components.CustomTimeBoxField
import com.teka.angaahewa.utils.ui_components.CustomTopAppBar
import com.teka.angaahewa.utils.ui_components.HandleSnackbarMessages
import com.teka.angaahewa.utils.ui_components.SnackbarManagerWithEncoding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionForm(
    navController: NavController,
    viewModel: CollectionFormViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val collectionFormUiState = viewModel.collectionFormUiState.collectAsState().value
    val roleOptions = listOf("Admin", "CM", "Agent", "Loader", "Field Officer")
    val fieldErrors = collectionFormUiState.fieldErrors

    // Replace the old snackbar setup with the new one
    val hostState = remember { SnackbarHostState() }
    val snackbarManager = remember {
        SnackbarManagerWithEncoding(
            hostState = hostState,
            haptic = haptic
        )
    }

    // Handle snackbar messages using the same pattern as login
    HandleSnackbarMessages(
        snackbarManager = snackbarManager,
        errorMessage = collectionFormUiState.errorMessage,
        successMessage = if (collectionFormUiState.isFormSubmissionSuccessful) "Collection Created Successfully" else null,
        onClearError = { viewModel.clearErrorMessage() },
        onClearSuccess = { viewModel.resetFormSubmissionState() }
    )

    // Handle successful form submission navigation
    LaunchedEffect(collectionFormUiState.isFormSubmissionSuccessful) {
        if (collectionFormUiState.isFormSubmissionSuccessful) {
            navController.popBackStack()
        }
    }

    // Handle suppliers error
    LaunchedEffect(collectionFormUiState.suppliersError) {
        collectionFormUiState.suppliersError?.let { message ->
            viewModel.updateUiState { copy(errorMessage = "Suppliers Error: $message") }
        }
    }

    // Handle transporters error
    LaunchedEffect(collectionFormUiState.transportersError) {
        collectionFormUiState.transportersError?.let { message ->
            viewModel.updateUiState { copy(errorMessage = "Transporters Error: $message") }
        }
    }

    // Validators for each field
    val supplierIdValidator: (String) -> String? = { value ->
        if (value.isBlank()) "Supplier ID is required" else {
            viewModel.clearFieldError("supplierId")
            null
        }
    }

    val supplyNumberValidator: (String) -> String? = { value ->
        if (value.isBlank()) "Supply Number is required" else {
            viewModel.clearFieldError("supplyNumber")
            null
        }
    }

    val weightValidator: (String) -> String? = { value ->
        when {
            value.isBlank() -> "Weight is required"
            value.toIntOrNull() == null -> "Weight must be a valid number"
            else -> {
                viewModel.clearFieldError("weight")
                null
            }
        }
    }

    val transporterIdValidator: (String) -> String? = { value ->
        when {
            value.isBlank() -> "Transporter ID is required"
            value.toIntOrNull() == null -> "Transporter ID must be a valid number"
            else -> {
                viewModel.clearFieldError("transporterId")
                null
            }
        }
    }

    val fieldAgentIdValidator: (String) -> String? = { value ->
        if (value.isBlank()) "Field Agent ID is required" else {
            viewModel.clearFieldError("fieldAgentId")
            null
        }
    }

    val latitudeValidator: (String) -> String? = { value ->
        when {
            value.isBlank() -> "Latitude is required"
            value.toDoubleOrNull() == null -> "Latitude must be a valid number"
            else -> {
                viewModel.clearFieldError("latitude")
                null
            }
        }
    }

    val longitudeValidator: (String) -> String? = { value ->
        when {
            value.isBlank() -> "Longitude is required"
            value.toDoubleOrNull() == null -> "Longitude must be a valid number"
            else -> {
                viewModel.clearFieldError("longitude")
                null
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        snackbarHost = { CustomSnackbarHost(hostState = snackbarManager.hostState) },
        topBar = {
            CustomTopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Collections Form",
                            fontSize = TextSizeXLarge,
                            modifier = Modifier.padding(0.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        enabled = !collectionFormUiState.isLoading // Disable back button while loading
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding(), start = 4.dp, end = 4.dp)
        ) {

            Box {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomDateBoxField(
                                modifier = Modifier.weight(1f),
                                currentTextState = collectionFormUiState.date.date.toString(),
                                onClick = {},
                                textStyle = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 16.sp,
                                ),
                                shape = CustomShapes.small
                            )
                            CustomTimeBoxField(
                                modifier = Modifier.weight(1f),
                                currentTextState = collectionFormUiState.time.formattedTimeBasedOnTimeFormat(12),
                                onClick = { },
                                textStyle = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 16.sp,
                                ),
                                shape = CustomShapes.medium
                            )
                        }
                    }

                    item {
                        // Supplier selection with loading indicator
                        Box {
                            BottomSheetTextField(
                                labelText = "Supplier",
                                placeholderText = if (collectionFormUiState.isLoadingSuppliers) "Loading suppliers..." else "Supplier",
                                currentTextState = collectionFormUiState.selectedSupplier?.name.orEmpty(),
                                onClick = {
                                    if (!collectionFormUiState.isLoadingSuppliers) {
                                        viewModel.updateUiState { copy(showSuppliersBottomSheet = true) }
                                    }
                                },
                                isOptional = true
                            )

                            // Show loading indicator for suppliers
                            if (collectionFormUiState.isLoadingSuppliers) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomInputTextField2(
                                modifier = Modifier.weight(1f),
                                labelText = "Supply Number",
                                value = collectionFormUiState.supplyNumber ?: "",
                                onValueChange = { value ->
                                    viewModel.updateUiState { copy(supplyNumber = value) }
                                },
                                onValidate = supplyNumberValidator,
                                isOptional = false,
                                enableRealTimeValidation = true,
                                errorMessage = fieldErrors["supplyNumber"],
                                enabled = !collectionFormUiState.isLoading
                            )

                            CustomInputTextField2(
                                modifier = Modifier.weight(1f),
                                labelText = "Weight (kg)",
                                value = collectionFormUiState.weight ?: "",
                                onValueChange = { value ->
                                    viewModel.updateUiState { copy(weight = value) }
                                },
                                onValidate = weightValidator,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isOptional = false,
                                enableRealTimeValidation = true,
                                errorMessage = fieldErrors["weight"],
                                enabled = !collectionFormUiState.isLoading
                            )
                        }
                    }

                    item {
                        // Transporter selection with loading indicator
                        Box {
                            BottomSheetTextField(
                                labelText = "Transporter",
                                placeholderText = if (collectionFormUiState.isLoadingTransporters) "Loading transporters..." else "Transporter",
                                currentTextState = collectionFormUiState.selectedTransporter?.name.orEmpty(),
                                onClick = {
                                    if (!collectionFormUiState.isLoadingTransporters) {
                                        viewModel.updateUiState { copy(showTransportersBottomSheet = true) }
                                    }
                                },
                                isOptional = true
                            )

                            // Show loading indicator for transporters
                            if (collectionFormUiState.isLoadingTransporters) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        CustomButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.createCollection() },
                            btnText = "Save Collection",
                            enabled = !collectionFormUiState.isLoading &&
                                    !collectionFormUiState.isLoadingSuppliers &&
                                    !collectionFormUiState.isLoadingTransporters,
                            isLoading = collectionFormUiState.isLoading,
                            loadingText = "Saving..."
                        )

                        // Show loading indicator on the button
                        if (collectionFormUiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                    }

                    item {
                        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                    }
                }
            }

            // Suppliers Bottom Sheet
            BottomSheetSelection(
                visible = collectionFormUiState.showSuppliersBottomSheet,
                title = "Select Supplier",
                items = collectionFormUiState.suppliers,
                searchValue = "",
                onSearchValueChange = {},
                onDismissRequest = {
                    viewModel.updateUiState { copy(showSuppliersBottomSheet = false) }
                },
                onItemSelected = { selectedSupplier ->
                    viewModel.updateUiState {
                        copy(
                            selectedSupplier = selectedSupplier,
                            showSuppliersBottomSheet = false
                        )
                    }
                    viewModel.clearFieldError("supplierId")
                },
                itemContent = { supplier ->
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryGreen
                    )
                }
            )

            // Transporters Bottom Sheet
            BottomSheetSelection(
                visible = collectionFormUiState.showTransportersBottomSheet,
                title = "Select Transporter",
                items = collectionFormUiState.transporters,
                searchValue = "",
                onSearchValueChange = {},
                onDismissRequest = {
                    viewModel.updateUiState { copy(showTransportersBottomSheet = false) }
                },
                onItemSelected = { selectedTransporter ->
                    viewModel.updateUiState {
                        copy(
                            selectedTransporter = selectedTransporter,
                            showTransportersBottomSheet = false
                        )
                    }
                    viewModel.clearFieldError("transporterId")
                },
                itemContent = { transporter ->
                    Text(
                        text = transporter.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryGreen
                    )
                }
            )
        }
    }
}