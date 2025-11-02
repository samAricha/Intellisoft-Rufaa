package com.teka.rufaa.modules.vitals.overweight_assesment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import com.teka.rufaa.data_layer.persistence.room.OverweightAssessmentDao
import com.teka.rufaa.data_layer.persistence.room.entities.OverweightAssessment
import com.teka.rufaa.modules.vitals.general_assesment.AssessmentResponseDto
import com.teka.rufaa.modules.vitals.general_assesment.ValidationErrorResponse
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
    private val appContext: Context,
    private val overweightAssessmentDao: OverweightAssessmentDao
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
                // Step 1: Save to local database first
                val assessmentEntity = OverweightAssessment(
                    patientId = patientId.trim(),
                    visitDate = visitDate,
                    generalHealth = generalHealth,
                    currentlyUsingDrugs = currentlyUsingDrugs,
                    comments = comments.trim(),
                    formType = "B",
                    isSynced = false
                )

                overweightAssessmentDao.insert(assessmentEntity)
                Timber.tag("OverweightAssessmentVM").i("Overweight assessment saved to local database")

                // Step 2: Attempt to sync with backend
                syncAssessmentToBackend(assessmentEntity)

            } catch (e: Exception) {
                Timber.tag("OverweightAssessmentVM").e("Exception during local save: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save assessment locally: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun syncAssessmentToBackend(assessment: OverweightAssessment) {
        try {
            val apiService = RetrofitProvider.simpleApiService(appContext)

            val request = OverweightAssessmentRequest(
                visit_date = assessment.visitDate,
                general_health = assessment.generalHealth,
                currently_using_drugs = assessment.currentlyUsingDrugs,
                comments = assessment.comments,
                patient_id = assessment.patientId,
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

            Timber.tag("OverweightAssessmentVM").i("Backend Response Code: ${response.code()}")

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                Timber.tag("OverweightAssessmentVM").i("Response Body: $responseBody")

                if (responseBody != null) {
                    val assessmentResponse = json.decodeFromString<AssessmentResponseDto>(responseBody)

                    if (assessmentResponse.success) {
                        // Update local record as synced
                        overweightAssessmentDao.markAsSynced(
                            assessment.id,
                            assessmentResponse.data.id,
                            assessmentResponse.data.visit_id
                        )
                        Timber.tag("OverweightAssessmentVM").i("Assessment synced successfully with server")

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = assessmentResponse.data.message,
                                assessmentSuccess = true
                            )
                        }
                    } else {
                        // Mark sync error but data is saved locally
                        overweightAssessmentDao.updateSyncError(assessment.id, assessmentResponse.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Assessment saved locally. Sync failed: ${assessmentResponse.message}",
                                assessmentSuccess = true
                            )
                        }
                    }
                } else {
                    overweightAssessmentDao.updateSyncError(assessment.id, "Empty response from server")
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
                Timber.tag("OverweightAssessmentVM").e("Sync Error: $errorBody")

                // Handle validation errors
                if (response.code() == 422 && errorBody != null) {
                    try {
                        val validationError = json.decodeFromString<ValidationErrorResponse>(errorBody)
                        overweightAssessmentDao.updateSyncError(assessment.id, validationError.message)

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
                        overweightAssessmentDao.updateSyncError(assessment.id, errorBody)
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

                    overweightAssessmentDao.updateSyncError(assessment.id, errorMsg)
                    Timber.tag("OverweightAssessmentVM").w("Sync failed: $errorMsg. Data saved locally.")

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
            Timber.tag("OverweightAssessmentVM").e("Backend sync exception: ${e.localizedMessage}")
            overweightAssessmentDao.updateSyncError(assessment.id, e.localizedMessage)

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
                val unsyncedAssessments = overweightAssessmentDao.getUnsyncedAssessments()
                Timber.tag("OverweightAssessmentVM").i("Found ${unsyncedAssessments.size} unsynced assessments")

                unsyncedAssessments.forEach { assessment ->
                    syncAssessmentToBackend(assessment)
                }
            } catch (e: Exception) {
                Timber.tag("OverweightAssessmentVM").e("Error syncing unsynced assessments: ${e.localizedMessage}")
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