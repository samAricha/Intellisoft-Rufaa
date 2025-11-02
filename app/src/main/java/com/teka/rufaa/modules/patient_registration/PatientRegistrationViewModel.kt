package com.teka.rufaa.modules.patient_registration

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
    private val appContext: Context
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
                val apiService = RetrofitProvider.simpleApiService(appContext)
                
                val request = PatientRegistrationRequest(
                    firstname = firstname.trim(),
                    lastname = lastname.trim(),
                    unique = unique.trim(),
                    dob = dob,
                    gender = gender,
                    reg_date = regDate
                )

                val requestBody = json.encodeToString(
                    PatientRegistrationRequest.serializer(),
                    request
                ).toRequestBody("application/json".toMediaType())

                val response = apiService.post(
                    url = AppEndpoints.PATIENTS_REGISTER,
                    body = requestBody
                )

                Timber.tag("PatientRegVM").i("Response: $response")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Timber.tag("PatientRegVM").i("Response Body: $responseBody")

                    if (responseBody != null) {
                        val registrationResponse = json.decodeFromString<PatientRegistrationResponseDto>(responseBody)
                        
                        if (registrationResponse.success) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = registrationResponse.data.message,
                                    registrationSuccess = true
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = registrationResponse.message
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
                    Timber.tag("PatientRegVM").e("Error: $errorBody")

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
                                errorMessage = errorBody ?: "Failed to register patient"
                            )
                        }

                        when (response.code()) {
                            401 -> {
                                Timber.tag("PatientRegVM").i("Unauthorized: Please login again")
                            }
                            503 -> {
                                Timber.tag("PatientRegVM").i("Service Unavailable: No internet connection")
                            }
                            else -> {
                                Timber.tag("PatientRegVM").e("HTTP error: ${response.code()}")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.tag("PatientRegVM").e("Exception: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to register patient: ${e.localizedMessage}"
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

    fun resetRegistrationSuccess() {
        _uiState.update { it.copy(registrationSuccess = false) }
    }
}