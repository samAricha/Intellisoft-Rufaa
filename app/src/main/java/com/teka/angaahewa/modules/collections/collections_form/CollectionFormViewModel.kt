package com.teka.angaahewa.modules.collections.collections_form

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.angaahewa.data_layer.DataStoreRepository
import com.teka.angaahewa.domain.CollectionResponse
import com.teka.angaahewa.domain.CreateCollectionRequest
import com.teka.angaahewa.domain.Supplier
import com.teka.angaahewa.domain.Transporter
import com.teka.angaahewa.utils.today
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.*
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import javax.inject.Inject

private const val CollectionForm_VM_TAG = "CollectionForm_VM_TAG"

@HiltViewModel
class CollectionFormViewModel @Inject constructor(
    private val appContext: Context,
    private val dataStoreRepository: DataStoreRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    // UI state holder
    private val _collectionFormUiState = MutableStateFlow(CollectionFormUiState())
    val collectionFormUiState: StateFlow<CollectionFormUiState> = _collectionFormUiState

    init {
        fetchSuppliers()
        fetchTransporters()
        loadFieldAgentId()
    }

    fun updateUiState(
        update: CollectionFormUiState.() -> CollectionFormUiState
    ) {
        _collectionFormUiState.update { it.update() }
    }

    private fun loadFieldAgentId() {
        viewModelScope.launch {
            try {
                val fieldAgent = dataStoreRepository.getFieldAgentData().first()
                val fieldAgentId = fieldAgent?.id

                Log.d(CollectionForm_VM_TAG, "Field agent ID loaded from DataStore: $fieldAgentId")

                _collectionFormUiState.update {
                    it.copy(fieldAgentId = fieldAgentId)
                }
            } catch (e: Exception) {
                Log.e(CollectionForm_VM_TAG, "Error loading field agent ID from DataStore", e)
                _collectionFormUiState.update {
                    it.copy(
                        errorMessage = "Failed to load field agent information: ${e.message}"
                    )
                }
            }
        }
    }

    private fun fetchSuppliers() {
        viewModelScope.launch {
            _collectionFormUiState.update {
                it.copy(isLoadingSuppliers = true, suppliersError = null)
            }

            try {
                val response = supabase.functions.invoke(
                    function = "suppliers"
                ) {
                    method = HttpMethod.Get
                }

                if (response.status.value in 200..299) {
                    val data = response.body<List<Supplier>>()
                    Log.d(CollectionForm_VM_TAG, "Suppliers fetched: $data")
                    _collectionFormUiState.update {
                        it.copy(
                            suppliers = data,
                            isLoadingSuppliers = false,
                            suppliersError = null
                        )
                    }
                } else {
                    val errorMessage = "Failed to fetch suppliers: ${response.status}"
                    Log.e(CollectionForm_VM_TAG, errorMessage)
                    _collectionFormUiState.update {
                        it.copy(
                            isLoadingSuppliers = false,
                            suppliersError = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error occurred while fetching suppliers"
                Log.e(CollectionForm_VM_TAG, "Error fetching suppliers", e)
                e.printStackTrace()
                _collectionFormUiState.update {
                    it.copy(
                        isLoadingSuppliers = false,
                        suppliersError = errorMessage
                    )
                }
            }
        }
    }

    private fun fetchTransporters() {
        viewModelScope.launch {
            _collectionFormUiState.update {
                it.copy(isLoadingTransporters = true, transportersError = null)
            }

            try {
                val response = supabase.functions.invoke(
                    function = "transporters"
                ) {
                    method = HttpMethod.Get
                }

                if (response.status.value in 200..299) {
                    val data = response.body<List<Transporter>>()
                    Log.d(CollectionForm_VM_TAG, "Transporters fetched: $data")
                    _collectionFormUiState.update {
                        it.copy(
                            transporters = data,
                            isLoadingTransporters = false,
                            transportersError = null
                        )
                    }
                } else {
                    val errorMessage = "Failed to fetch transporters: ${response.status}"
                    Log.e(CollectionForm_VM_TAG, errorMessage)
                    _collectionFormUiState.update {
                        it.copy(
                            isLoadingTransporters = false,
                            transportersError = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error occurred while fetching transporters"
                Log.e(CollectionForm_VM_TAG, "Error fetching transporters", e)
                e.printStackTrace()
                _collectionFormUiState.update {
                    it.copy(
                        isLoadingTransporters = false,
                        transportersError = errorMessage
                    )
                }
            }
        }
    }

    fun retryFetchSuppliers() {
        Log.d(CollectionForm_VM_TAG, "Retrying suppliers fetch")
        fetchSuppliers()
    }

    fun retryFetchTransporters() {
        Log.d(CollectionForm_VM_TAG, "Retrying transporters fetch")
        fetchTransporters()
    }

    fun createCollection() {
        viewModelScope.launch {
            val currentState = _collectionFormUiState.value

            // Validate required fields
            val errors = validateForm(currentState)
            if (errors.isNotEmpty()) {
                Log.w(CollectionForm_VM_TAG, "Form validation failed: $errors")
                _collectionFormUiState.update {
                    it.copy(
                        fieldErrors = errors,
                        errorMessage = "Please fix the form errors before submitting"
                    )
                }
                return@launch
            }

            // Set loading state
            _collectionFormUiState.update {
                it.copy(
                    isLoading = true,
                    fieldErrors = emptyMap(),
                    errorMessage = null,
                    successMessage = null,
                    isFormSubmissionSuccessful = false
                )
            }

            try {
                // Create your collection request
                val request = CreateCollectionRequest(
                    supplier_id = currentState.selectedSupplier?.id ?: "",
                    supply_number = currentState.supplyNumber ?: "",
                    weight_kg = currentState.weight?.toIntOrNull() ?: 0,
                    transporter_id = currentState.selectedTransporter?.id ?: 0,
                    field_agent_id = currentState.fieldAgentId ?: ""
                )

                Log.d(CollectionForm_VM_TAG, "Creating collection with request: $request")


                // Make POST request to collections endpoint
                val response = supabase.functions.invoke("collections") {
                    method = HttpMethod.Post
                    header(HttpHeaders.ContentType, "application/json")
                    setBody(request)
                }

                Log.d(CollectionForm_VM_TAG, "Raw Collection Response: $response")

                if (response.status.value in 200..299) {
                    val collectionResponse = response.body<CollectionResponse>()
                    Log.d(CollectionForm_VM_TAG, "Collection created successfully: $collectionResponse")

                    _collectionFormUiState.update {
                        it.copy(
                            isLoading = false,
                            isFormSubmissionSuccessful = true,
                            successMessage = "Collection created successfully!",
                            errorMessage = null
                        )
                    }
                } else {
                    val errorMessage = when (response.status.value) {
                        400 -> "Invalid data provided. Please check your inputs."
                        401 -> "Authentication failed. Please login again."
                        403 -> "You don't have permission to create collections."
                        404 -> "Service not found. Please try again later."
                        409 -> "A collection with this supply number already exists."
                        422 -> "Invalid data format. Please check your inputs."
                        500 -> "Server error. Please try again later."
                        else -> "Failed to create collection: ${response.status.description}"
                    }
                    Log.e(CollectionForm_VM_TAG, "Failed to create collection: ${response.status}")
                    _collectionFormUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage,
                            successMessage = null,
                            isFormSubmissionSuccessful = false
                        )
                    }
                }

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Request timed out. Please try again."
                    else -> "An unexpected error occurred. Please try again."
                }
                Log.e(CollectionForm_VM_TAG, "Error creating collection", e)
                e.printStackTrace()
                _collectionFormUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMessage,
                        successMessage = null,
                        isFormSubmissionSuccessful = false
                    )
                }
            }
        }
    }

    fun clearFieldError(fieldName: String) {
        _collectionFormUiState.update {
            it.copy(
                fieldErrors = it.fieldErrors.toMutableMap().apply {
                    remove(fieldName)
                }
            )
        }
    }

    fun clearAllFieldErrors() {
        _collectionFormUiState.update {
            it.copy(fieldErrors = emptyMap())
        }
    }

    fun clearErrorMessage() {
        _collectionFormUiState.update {
            it.copy(errorMessage = null)
        }
    }

    fun clearSuccessMessage() {
        _collectionFormUiState.update {
            it.copy(successMessage = null)
        }
    }

    fun resetFormSubmissionState() {
        _collectionFormUiState.update {
            it.copy(
                isFormSubmissionSuccessful = false,
                successMessage = null
            )
        }
    }

    private fun validateForm(state: CollectionFormUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (state.selectedSupplier == null) {
            errors["supplierId"] = "Supplier is required"
        }

        if (state.supplyNumber.isNullOrBlank()) {
            errors["supplyNumber"] = "Supply Number is required"
        }

        if (state.weight.isNullOrBlank()) {
            errors["weight"] = "Weight is required"
        } else {
            state.weight.toIntOrNull() ?: run {
                errors["weight"] = "Weight must be a valid number"
            }
        }

        if (state.selectedTransporter == null) {
            errors["transporterId"] = "Transporter is required"
        }

        if (state.fieldAgentId.isNullOrBlank()) {
            errors["fieldAgentId"] = "Field Agent not found. Please ensure you are logged in as a field agent."
        }

        return errors
    }
}

data class CollectionFormUiState(
    var categoryList: List<String> = mutableListOf("Processed", "Dumped", "Donation", "Local Sale", "Sample", "Kephis", "Kitchen", "Gift"),
    var receiverList: List<String> = mutableListOf("Medium Care", "Standard Care", "Frozen", "High Care", "N/A"),

    var date: LocalDateTime = today(),
    var time: LocalTime = today().time,

    // Form fields
    val supplyNumber: String? = null,
    val weight: String? = null,
    val fieldAgentId: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,

    // UI state for form submission
    val isLoading: Boolean = false,
    val isFormSubmissionSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),

    // UI state for bottom sheets
    val showSuppliersBottomSheet: Boolean = false,
    val showTransportersBottomSheet: Boolean = false,

    // Suppliers state
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplier: Supplier? = null,
    val isLoadingSuppliers: Boolean = false,
    val suppliersError: String? = null,

    // Transporters state
    val transporters: List<Transporter> = emptyList(),
    val selectedTransporter: Transporter? = null,
    val isLoadingTransporters: Boolean = false,
    val transportersError: String? = null
)