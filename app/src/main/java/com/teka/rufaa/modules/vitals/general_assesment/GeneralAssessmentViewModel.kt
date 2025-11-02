package com.teka.rufaa.modules.vitals.general_assesment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import com.teka.rufaa.data_layer.persistence.room.GeneralAssessmentDao
import com.teka.rufaa.data_layer.persistence.room.entities.GeneralAssessment
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
    private val appContext: Context,
    private val generalAssessmentDao: GeneralAssessmentDao
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
                // Step 1: Save to local database first
                val assessmentEntity = GeneralAssessment(
                    patientId = patientId.trim(),
                    visitDate = visitDate,
                    generalHealth = generalHealth,
                    onDietToLoseWeight = onDietToLoseWeight,
                    comments = comments.trim(),
                    formType = "A",
                    isSynced = false
                )

                generalAssessmentDao.insert(assessmentEntity)
                Timber.tag("GeneralAssessmentVM").i("General assessment saved to local database")

                // Step 2: Attempt to sync with backend
                syncAssessmentToBackend(assessmentEntity)

            } catch (e: Exception) {
                Timber.tag("GeneralAssessmentVM").e("Exception during local save: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save assessment locally: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun syncAssessmentToBackend(assessment: GeneralAssessment) {
        try {
            val apiService = RetrofitProvider.simpleApiService(appContext)

            val request = GeneralAssessmentRequest(
                visit_date = assessment.visitDate,
                general_health = assessment.generalHealth,
                on_diet_to_lose_weight = assessment.onDietToLoseWeight,
                comments = assessment.comments,
                patient_id = assessment.patientId,
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

            Timber.tag("GeneralAssessmentVM").i("Backend Response Code: ${response.code()}")

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                Timber.tag("GeneralAssessmentVM").i("Response Body: $responseBody")

                if (responseBody != null) {
                    val assessmentResponse = json.decodeFromString<AssessmentResponseDto>(responseBody)

                    if (assessmentResponse.success) {
                        // Update local record as synced
                        generalAssessmentDao.markAsSynced(
                            assessment.id,
                            assessmentResponse.data.id,
                            assessmentResponse.data.visit_id
                        )
                        Timber.tag("GeneralAssessmentVM").i("Assessment synced successfully with server")

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = assessmentResponse.data.message,
                                assessmentSuccess = true
                            )
                        }
                    } else {
                        // Mark sync error but data is saved locally
                        generalAssessmentDao.updateSyncError(assessment.id, assessmentResponse.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Assessment saved locally. Sync failed: ${assessmentResponse.message}",
                                assessmentSuccess = true
                            )
                        }
                    }
                } else {
                    generalAssessmentDao.updateSyncError(assessment.id, "Empty response from server")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Assessment saved locally. Will sync later.",
                            assessmentSuccess = true
                        )
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.tag("GeneralAssessmentVM").e("Sync Error: $errorBody")

                // Handle validation errors
                if (response.code() == 422 && errorBody != null) {
                    try {
                        val validationError = json.decodeFromString<ValidationErrorResponse>(errorBody)
                        generalAssessmentDao.updateSyncError(assessment.id, validationError.message)

                        val fieldErrors = validationError.errors.mapValues { entry ->
                            entry.value.firstOrNull() ?: "Invalid value"
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                validationErrors = fieldErrors,
                                errorMessage = "Assessment saved locally but sync failed: ${validationError.message}"
                            )
                        }
                    } catch (e: Exception) {
                        generalAssessmentDao.updateSyncError(assessment.id, errorBody)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Assessment saved locally. Will retry sync later.",
                                assessmentSuccess = true
                            )
                        }
                    }
                } else {
                    // Save error but allow continuation since data is saved locally
                    val errorMsg = when (response.code()) {
                        401 -> "Unauthorized - Please login again"
                        503 -> "Service unavailable - No internet connection"
                        else -> errorBody ?: "Network error"
                    }

                    generalAssessmentDao.updateSyncError(assessment.id, errorMsg)
                    Timber.tag("GeneralAssessmentVM").w("Sync failed: $errorMsg. Data saved locally.")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Assessment saved locally. Will sync when online.",
                            assessmentSuccess = true
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Timber.tag("GeneralAssessmentVM").e("Backend sync exception: ${e.localizedMessage}")
            generalAssessmentDao.updateSyncError(assessment.id, e.localizedMessage)

            // Data is saved locally, so we still consider it a success
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = "Assessment saved locally. Will sync when connection is available.",
                    assessmentSuccess = true
                )
            }
        }
    }

    /**
     * Manually sync unsynced assessments
     */
    fun syncUnsyncedAssessments() {
        viewModelScope.launch {
            try {
                val unsyncedAssessments = generalAssessmentDao.getUnsyncedAssessments()
                Timber.tag("GeneralAssessmentVM").i("Found ${unsyncedAssessments.size} unsynced assessments")

                unsyncedAssessments.forEach { assessment ->
                    syncAssessmentToBackend(assessment)
                }
            } catch (e: Exception) {
                Timber.tag("GeneralAssessmentVM").e("Error syncing unsynced assessments: ${e.localizedMessage}")
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