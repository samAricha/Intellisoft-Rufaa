package com.teka.rufaa.modules.vitals

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
import kotlin.math.pow

@Serializable
data class VitalsRequest(
    val visit_date: String,
    val height: String,
    val weight: String,
    val bmi: String,
    val patient_id: String
)

@Serializable
data class VitalsData(
    val id: Int,
    val patient_id: String,
    val slug: Int,
    val message: String
)

@Serializable
data class VitalsResponseDto(
    val message: String,
    val success: Boolean,
    val code: Int,
    val data: VitalsData
)

@Serializable
data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>
)

data class VitalsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val vitalsSuccess: Boolean = false,
    val calculatedBmi: Double? = null,
    val bmiCategory: BmiCategory = BmiCategory.UNKNOWN,
    val assessmentRoute: AssessmentRoute = AssessmentRoute.UNKNOWN
)

enum class BmiCategory {
    UNDERWEIGHT, // BMI < 18.5
    NORMAL,      // 18.5 <= BMI <= 25
    OVERWEIGHT,  // BMI > 25
    UNKNOWN
}

enum class AssessmentRoute {
    GENERAL,     // BMI <= 25
    OVERWEIGHT,  // BMI > 25
    UNKNOWN
}

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Calculate BMI and determine category
     * BMI = weight(kg) / height(m)²
     */
    fun calculateBmi(heightCm: String, weightKg: String) {
        if (heightCm.isBlank() || weightKg.isBlank()) {
            _uiState.update {
                it.copy(
                    calculatedBmi = null,
                    bmiCategory = BmiCategory.UNKNOWN
                )
            }
            return
        }

        try {
            val height = heightCm.toDoubleOrNull()
            val weight = weightKg.toDoubleOrNull()

            if (height == null || weight == null || height <= 0 || weight <= 0) {
                _uiState.update {
                    it.copy(
                        calculatedBmi = null,
                        bmiCategory = BmiCategory.UNKNOWN
                    )
                }
                return
            }

            // Convert height from cm to meters
            val heightInMeters = height / 100.0

            // Calculate BMI
            val bmi = weight / heightInMeters.pow(2)

            // Determine BMI category
            val category = when {
                bmi < 18.5 -> BmiCategory.UNDERWEIGHT
                bmi <= 25.0 -> BmiCategory.NORMAL
                else -> BmiCategory.OVERWEIGHT
            }

            // Determine assessment route based on requirements:
            // BMI <= 25 → General Assessment
            // BMI > 25 → Overweight Assessment
            val route = if (bmi <= 25.0) {
                AssessmentRoute.GENERAL
            } else {
                AssessmentRoute.OVERWEIGHT
            }

            _uiState.update {
                it.copy(
                    calculatedBmi = bmi,
                    bmiCategory = category,
                    assessmentRoute = route
                )
            }
        } catch (e: Exception) {
            Timber.tag("VitalsVM").e("BMI calculation error: ${e.localizedMessage}")
            _uiState.update {
                it.copy(
                    calculatedBmi = null,
                    bmiCategory = BmiCategory.UNKNOWN
                )
            }
        }
    }

    fun submitVitals(
        visitDate: String,
        height: String,
        weight: String,
        patientId: String
    ) {
        // Clear previous errors
        _uiState.update { it.copy(validationErrors = emptyMap(), errorMessage = null) }

        // Client-side validation
        val errors = mutableMapOf<String, String>()

        if (visitDate.isBlank()) {
            errors["visit_date"] = "Visit date is required"
        }
        if (height.isBlank()) {
            errors["height"] = "Height is required"
        } else {
            val heightValue = height.toDoubleOrNull()
            if (heightValue == null || heightValue <= 0) {
                errors["height"] = "Please enter a valid height"
            } else if (heightValue > 300) {
                errors["height"] = "Height seems unrealistic (max 300cm)"
            }
        }
        if (weight.isBlank()) {
            errors["weight"] = "Weight is required"
        } else {
            val weightValue = weight.toDoubleOrNull()
            if (weightValue == null || weightValue <= 0) {
                errors["weight"] = "Please enter a valid weight"
            } else if (weightValue > 500) {
                errors["weight"] = "Weight seems unrealistic (max 500kg)"
            }
        }
        if (patientId.isBlank()) {
            errors["patient_id"] = "Patient ID is required"
        }

        // Check if BMI is calculated
        if (_uiState.value.calculatedBmi == null) {
            errors["bmi"] = "Unable to calculate BMI. Please check height and weight."
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val apiService = RetrofitProvider.simpleApiService(appContext)

                // Format BMI to 1 decimal place
                val bmiValue = _uiState.value.calculatedBmi?.let {
                    String.format("%.1f", it)
                } ?: "0.0"

                val request = VitalsRequest(
                    visit_date = visitDate,
                    height = height.trim(),
                    weight = weight.trim(),
                    bmi = bmiValue,
                    patient_id = patientId.trim()
                )

                val requestBody = json.encodeToString(
                    VitalsRequest.serializer(),
                    request
                ).toRequestBody("application/json".toMediaType())

                val response = apiService.post(
                    url = AppEndpoints.VITALS_ADD,
                    body = requestBody
                )

                Timber.tag("VitalsVM").i("Response: $response")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Timber.tag("VitalsVM").i("Response Body: $responseBody")

                    if (responseBody != null) {
                        val vitalsResponse = json.decodeFromString<VitalsResponseDto>(responseBody)

                        if (vitalsResponse.success) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = vitalsResponse.data.message,
                                    vitalsSuccess = true
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = vitalsResponse.message
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
                    Timber.tag("VitalsVM").e("Error: $errorBody")

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
                                errorMessage = errorBody ?: "Failed to submit vitals"
                            )
                        }

                        when (response.code()) {
                            401 -> {
                                Timber.tag("VitalsVM").i("Unauthorized: Please login again")
                            }
                            503 -> {
                                Timber.tag("VitalsVM").i("Service Unavailable: No internet connection")
                            }
                            else -> {
                                Timber.tag("VitalsVM").e("HTTP error: ${response.code()}")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.tag("VitalsVM").e("Exception: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to submit vitals: ${e.localizedMessage}"
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

    fun resetVitalsSuccess() {
        _uiState.update { it.copy(vitalsSuccess = false) }
    }

    fun getBmiCategoryText(): String {
        return when (_uiState.value.bmiCategory) {
            BmiCategory.UNDERWEIGHT -> "Underweight"
            BmiCategory.NORMAL -> "Normal"
            BmiCategory.OVERWEIGHT -> "Overweight"
            BmiCategory.UNKNOWN -> ""
        }
    }

    fun getBmiColor(): androidx.compose.ui.graphics.Color {
        return when (_uiState.value.bmiCategory) {
            BmiCategory.UNDERWEIGHT -> androidx.compose.ui.graphics.Color(0xFFFBBF24) // Yellow
            BmiCategory.NORMAL -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
            BmiCategory.OVERWEIGHT -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
            BmiCategory.UNKNOWN -> androidx.compose.ui.graphics.Color(0xFF6B7280) // Gray
        }
    }
}