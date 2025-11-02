package com.teka.rufaa.modules.vitals.general_assesment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject

@Serializable
data class GeneralAssessmentRequest(
    val visit_date: String,
    val general_health: String,
    val on_diet_to_lose_weight: String,
    val comments: String,
    val patient_id: String,
    val form_type: String = "A" // General Assessment is Form A
)

@Serializable
data class AssessmentData(
    val id: Int,
    val patient_id: String,
    val visit_id: Int,
    val message: String
)

@Serializable
data class AssessmentResponseDto(
    val message: String,
    val success: Boolean,
    val code: Int,
    val data: AssessmentData
)

@Serializable
data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>
)

data class GeneralAssessmentUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val assessmentSuccess: Boolean = false
)

@HiltViewModel
class GeneralAssessmentViewModel @Inject constructor(
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneralAssessmentUiState())
    val uiState: StateFlow<GeneralAssessmentUiState> = _uiState

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun submitGeneralAssessment(
        visitDate: String,
        generalHealth: String,
        onDietToLoseWeight: String,
        comments: String,
        patientId: String
    ) {
        // Clear previous errors
        _uiState.update { it.copy(validationErrors = emptyMap(), errorMessage = null) }

        // Client-side validation
        val errors = mutableMapOf<String, String>()
        
        if (visitDate.isBlank()) {
            errors["visit_date"] = "Visit date is required"
        }
        if (generalHealth.isBlank()) {
            errors["general_health"] = "General health status is required"
        }
        if (onDietToLoseWeight.isBlank()) {
            errors["on_diet_to_lose_weight"] = "Please answer the diet question"
        }
        if (comments.isBlank()) {
            errors["comments"] = "Comments are required"
        }
        if (patientId.isBlank()) {
            errors["patient_id"] = "Patient ID is required"
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val apiService = RetrofitProvider.simpleApiService(appContext)
                
                val request = GeneralAssessmentRequest(
                    visit_date = visitDate,
                    general_health = generalHealth,
                    on_diet_to_lose_weight = onDietToLoseWeight,
                    comments = comments.trim(),
                    patient_id = patientId.trim(),
                    form_type = "A"
                )

                val requestBody = json.encodeToString(
                    GeneralAssessmentRequest.serializer(),
                    request
                ).toRequestBody("application/json".toMediaType())

                val response = apiService.post(
                    url = AppEndpoints.VISITS_ADD,
                    body = requestBody
                )

                Timber.tag("GeneralAssessmentVM").i("Response: $response")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Timber.tag("GeneralAssessmentVM").i("Response Body: $responseBody")

                    if (responseBody != null) {
                        val assessmentResponse = json.decodeFromString<AssessmentResponseDto>(responseBody)
                        
                        if (assessmentResponse.success) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = assessmentResponse.data.message,
                                    assessmentSuccess = true
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = assessmentResponse.message
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Empty response from server"
                            )
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Timber.tag("GeneralAssessmentVM").e("Error: $errorBody")

                    // Handle validation errors
                    if (response.code() == 422 && errorBody != null) {
                        try {
                            val validationError = json.decodeFromString<ValidationErrorResponse>(errorBody)
                            val fieldErrors = validationError.errors.mapValues { entry ->
                                entry.value.firstOrNull() ?: "Invalid value"
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    validationErrors = fieldErrors,
                                    errorMessage = validationError.message
                                )
                            }
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = errorBody
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = errorBody ?: "Failed to submit assessment"
                            )
                        }

                        when (response.code()) {
                            401 -> Timber.tag("GeneralAssessmentVM").i("Unauthorized: Please login again")
                            503 -> Timber.tag("GeneralAssessmentVM").i("Service Unavailable: No internet connection")
                            else -> Timber.tag("GeneralAssessmentVM").e("HTTP error: ${response.code()}")
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.tag("GeneralAssessmentVM").e("Exception: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to submit assessment: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearValidationError(field: String) {
        _uiState.update {
            val newErrors = it.validationErrors.toMutableMap()
            newErrors.remove(field)
            it.copy(validationErrors = newErrors)
        }
    }

    fun resetAssessmentSuccess() {
        _uiState.update { it.copy(assessmentSuccess = false) }
    }
}