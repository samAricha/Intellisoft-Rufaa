package com.teka.rufaa.modules.vitals.overweight_assesment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import com.teka.rufaa.modules.vitals.ValidationErrorResponse
import com.teka.rufaa.modules.vitals.general_assesment.AssessmentResponseDto
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
data class OverweightAssessmentRequest(
    val visit_date: String,
    val general_health: String,
    val currently_using_drugs: String,
    val comments: String,
    val patient_id: String,
    val form_type: String = "B" // Overweight Assessment is Form B
)

data class OverweightAssessmentUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val assessmentSuccess: Boolean = false
)

@HiltViewModel
class OverweightAssessmentViewModel @Inject constructor(
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverweightAssessmentUiState())
    val uiState: StateFlow<OverweightAssessmentUiState> = _uiState

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun submitOverweightAssessment(
        visitDate: String,
        generalHealth: String,
        currentlyUsingDrugs: String,
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
        if (currentlyUsingDrugs.isBlank()) {
            errors["currently_using_drugs"] = "Please answer the drugs question"
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
                
                val request = OverweightAssessmentRequest(
                    visit_date = visitDate,
                    general_health = generalHealth,
                    currently_using_drugs = currentlyUsingDrugs,
                    comments = comments.trim(),
                    patient_id = patientId.trim(),
                    form_type = "B"
                )

                val requestBody = json.encodeToString(
                    OverweightAssessmentRequest.serializer(),
                    request
                ).toRequestBody("application/json".toMediaType())

                val response = apiService.post(
                    url = AppEndpoints.VISITS_ADD,
                    body = requestBody
                )

                Timber.tag("OverweightAssessmentVM").i("Response: $response")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Timber.tag("OverweightAssessmentVM").i("Response Body: $responseBody")

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
                    Timber.tag("OverweightAssessmentVM").e("Error: $errorBody")

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
                            401 -> Timber.tag("OverweightAssessmentVM").i("Unauthorized: Please login again")
                            503 -> Timber.tag("OverweightAssessmentVM").i("Service Unavailable: No internet connection")
                            else -> Timber.tag("OverweightAssessmentVM").e("HTTP error: ${response.code()}")
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.tag("OverweightAssessmentVM").e("Exception: ${e.localizedMessage}")
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