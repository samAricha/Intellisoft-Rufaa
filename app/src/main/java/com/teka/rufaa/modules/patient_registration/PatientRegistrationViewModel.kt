package com.teka.rufaa.modules.patient_registration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import com.teka.rufaa.data_layer.persistence.room.PatientDao
import com.teka.rufaa.data_layer.persistence.room.entities.Patient
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
data class PatientRegistrationRequest(
    val firstname: String,
    val lastname: String,
    val unique: String,
    val dob: String,
    val gender: String,
    val reg_date: String
)

@Serializable
data class PatientRegistrationData(
    val proceed: Int,
    val message: String
)

@Serializable
data class PatientRegistrationResponseDto(
    val message: String,
    val success: Boolean,
    val code: Int,
    val data: PatientRegistrationData
)

@Serializable
data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>
)

data class PatientRegistrationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val registrationSuccess: Boolean = false
)

@HiltViewModel
class PatientRegistrationViewModel @Inject constructor(
    private val appContext: Context,
    private val patientDao: PatientDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientRegistrationUiState())
    val uiState: StateFlow<PatientRegistrationUiState> = _uiState

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun registerPatient(
        firstname: String,
        lastname: String,
        unique: String,
        dob: String,
        gender: String,
        regDate: String
    ) {
        // Clear previous errors
        _uiState.update { it.copy(validationErrors = emptyMap(), errorMessage = null) }

        // Client-side validation
        val errors = mutableMapOf<String, String>()

        if (firstname.isBlank()) {
            errors["firstname"] = "First name is required"
        }
        if (lastname.isBlank()) {
            errors["lastname"] = "Last name is required"
        }
        if (unique.isBlank()) {
            errors["unique"] = "Patient ID is required"
        }
        if (dob.isBlank()) {
            errors["dob"] = "Date of birth is required"
        }
        if (gender.isBlank()) {
            errors["gender"] = "Gender is required"
        }
        if (regDate.isBlank()) {
            errors["reg_date"] = "Registration date is required"
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Check if patient with this unique ID already exists locally
                val existingPatient = patientDao.getPatientByUniqueId(unique.trim())
                if (existingPatient != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            validationErrors = mapOf("unique" to "A patient with this ID already exists"),
                            errorMessage = "Patient ID must be unique"
                        )
                    }
                    return@launch
                }

                // Step 1: Save to local database first
                val patientEntity = Patient(
                    firstname = firstname.trim(),
                    lastname = lastname.trim(),
                    uniqueId = unique.trim(),
                    dob = dob,
                    gender = gender,
                    regDate = regDate,
                    isSynced = false
                )

                patientDao.insert(patientEntity)
                Timber.tag("PatientRegVM").i("Patient saved to local database")

                // Step 2: Attempt to sync with backend
                syncPatientToBackend(patientEntity)

            } catch (e: Exception) {
                Timber.tag("PatientRegVM").e("Exception during local save: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save patient locally: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private suspend fun syncPatientToBackend(patient: Patient) {
        try {
            val apiService = RetrofitProvider.simpleApiService(appContext)

            val request = PatientRegistrationRequest(
                firstname = patient.firstname,
                lastname = patient.lastname,
                unique = patient.uniqueId,
                dob = patient.dob,
                gender = patient.gender,
                reg_date = patient.regDate
            )

            val requestBody = json.encodeToString(
                PatientRegistrationRequest.serializer(),
                request
            ).toRequestBody("application/json".toMediaType())

            val response = apiService.post(
                url = AppEndpoints.PATIENTS_REGISTER,
                body = requestBody
            )

            Timber.tag("PatientRegVM").i("Backend Response Code: ${response.code()}")

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                Timber.tag("PatientRegVM").i("Response Body: $responseBody")

                if (responseBody != null) {
                    val registrationResponse = json.decodeFromString<PatientRegistrationResponseDto>(responseBody)

                    if (registrationResponse.success) {
                        // Update local record as synced
                        patientDao.markAsSynced(patient.id, registrationResponse.data.proceed)
                        Timber.tag("PatientRegVM").i("Patient synced successfully with server")

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = registrationResponse.data.message,
                                registrationSuccess = true
                            )
                        }
                    } else {
                        // Mark sync error but data is saved locally
                        patientDao.updateSyncError(patient.id, registrationResponse.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Patient saved locally. Sync failed: ${registrationResponse.message}",
                                registrationSuccess = true
                            )
                        }
                    }
                } else {
                    patientDao.updateSyncError(patient.id, "Empty response from server")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Patient saved locally. Will sync later.",
                            registrationSuccess = true
                        )
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.tag("PatientRegVM").e("Sync Error: $errorBody")

                // Handle validation errors
                if (response.code() == 422 && errorBody != null) {
                    try {
                        val validationError = json.decodeFromString<ValidationErrorResponse>(errorBody)
                        patientDao.updateSyncError(patient.id, validationError.message)

                        val fieldErrors = validationError.errors.mapValues { entry ->
                            entry.value.firstOrNull() ?: "Invalid value"
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                validationErrors = fieldErrors,
                                errorMessage = "Patient saved locally but sync failed: ${validationError.message}"
                            )
                        }
                    } catch (e: Exception) {
                        patientDao.updateSyncError(patient.id, errorBody)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMessage = "Patient saved locally. Will retry sync later.",
                                registrationSuccess = true
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

                    patientDao.updateSyncError(patient.id, errorMsg)
                    Timber.tag("PatientRegVM").w("Sync failed: $errorMsg. Data saved locally.")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Patient saved locally. Will sync when online.",
                            registrationSuccess = true
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Timber.tag("PatientRegVM").e("Backend sync exception: ${e.localizedMessage}")
            patientDao.updateSyncError(patient.id, e.localizedMessage)

            // Data is saved locally, so we still consider it a success
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = "Patient saved locally. Will sync when connection is available.",
                    registrationSuccess = true
                )
            }
        }
    }

    /**
     * Manually sync unsynced patients
     */
    fun syncUnsyncedPatients() {
        viewModelScope.launch {
            try {
                val unsyncedPatients = patientDao.getUnsyncedPatients()
                Timber.tag("PatientRegVM").i("Found ${unsyncedPatients.size} unsynced patients")

                unsyncedPatients.forEach { patient ->
                    syncPatientToBackend(patient)
                }
            } catch (e: Exception) {
                Timber.tag("PatientRegVM").e("Error syncing unsynced patients: ${e.localizedMessage}")
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

    fun resetRegistrationSuccess() {
        _uiState.update { it.copy(registrationSuccess = false) }
    }
}