package com.teka.rufaa.modules.patient_details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import com.teka.rufaa.modules.patients_list.PatientDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@Serializable
data class PatientDetailResponseDto(
    val message: String,
    val success: Boolean,
    val code: Int,
    val data: PatientDto
)

data class PatientDetailUiState(
    val isLoading: Boolean = false,
    val patient: PatientDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientDetailUiState())
    val uiState: StateFlow<PatientDetailUiState> = _uiState

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun loadPatientDetail(patientId: Int) {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val apiService = RetrofitProvider.simpleApiService(appContext)
                
                val response = apiService.get(
                    url = "${AppEndpoints.PATIENT_DETAILS}/$patientId"
                )

                Timber.tag("PatientDetailVM").i("Response: $response")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Timber.tag("PatientDetailVM").i("Response Body: $responseBody")

                    if (responseBody != null) {
                        val patientResponse = json.decodeFromString<PatientDetailResponseDto>(responseBody)
                        
                        if (patientResponse.success) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    patient = patientResponse.data,
                                    successMessage = "Patient details loaded"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = patientResponse.message
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
                    Timber.tag("PatientDetailVM").e("Error: $errorBody")
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorBody ?: "Failed to load patient details"
                        )
                    }

                    when (response.code()) {
                        401 -> {
                            Timber.tag("PatientDetailVM").i("Unauthorized: Please login again")
                        }
                        404 -> {
                            Timber.tag("PatientDetailVM").i("Patient not found")
                        }
                        503 -> {
                            Timber.tag("PatientDetailVM").i("Service Unavailable: No internet connection")
                        }
                        else -> {
                            Timber.tag("PatientDetailVM").e("HTTP error: ${response.code()}")
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.tag("PatientDetailVM").e("Exception: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load patient details: ${e.localizedMessage}"
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
}