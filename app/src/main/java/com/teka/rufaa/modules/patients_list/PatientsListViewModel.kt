package com.teka.rufaa.modules.patients_list

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
import timber.log.Timber
import javax.inject.Inject

@Serializable
data class PatientDto(
    val id: Int,
    val unique: String,
    val firstname: String,
    val lastname: String,
    val dob: String,
    val gender: String,
    val reg_date: String,
    val user_id: Int,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class PatientsResponseDto(
    val message: String,
    val success: Boolean,
    val code: Int,
    val data: List<PatientDto>
)

data class PatientsListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class PatientsListViewModel @Inject constructor(
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientsListUiState())
    val uiState: StateFlow<PatientsListUiState> = _uiState

    private val _patients = MutableStateFlow<List<PatientDto>>(emptyList())
    val patients: StateFlow<List<PatientDto>> = _patients

    private val _filteredPatients = MutableStateFlow<List<PatientDto>>(emptyList())
    val filteredPatients: StateFlow<List<PatientDto>> = _filteredPatients

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun loadPatients() {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val apiService = RetrofitProvider.simpleApiService(appContext)
                
                val response = apiService.get(
                    url = AppEndpoints.PATIENTS_VIEW
                )

                Timber.tag("PatientsVM").i("Response: $response")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Timber.tag("PatientsVM").i("Response Body: $responseBody")

                    if (responseBody != null) {
                        val patientsResponse = json.decodeFromString<PatientsResponseDto>(responseBody)
                        
                        if (patientsResponse.success) {
                            _patients.value = patientsResponse.data
                            _filteredPatients.value = patientsResponse.data
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Loaded ${patientsResponse.data.size} patients"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = patientsResponse.message
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
                    Timber.tag("PatientsVM").e("Error: $errorBody")
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorBody ?: "Failed to load patients"
                        )
                    }

                    when (response.code()) {
                        401 -> {
                            Timber.tag("PatientsVM").i("Unauthorized: Please login again")
                        }
                        503 -> {
                            Timber.tag("PatientsVM").i("Service Unavailable: No internet connection")
                        }
                        else -> {
                            Timber.tag("PatientsVM").e("HTTP error: ${response.code()}")
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.tag("PatientsVM").e("Exception: ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load patients: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun searchPatients(query: String) {
        if (query.isBlank()) {
            _filteredPatients.value = _patients.value
        } else {
            _filteredPatients.value = _patients.value.filter { patient ->
                patient.firstname.contains(query, ignoreCase = true) ||
                patient.lastname.contains(query, ignoreCase = true) ||
                patient.unique.contains(query, ignoreCase = true)
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