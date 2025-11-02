package com.teka.rufaa.modules.auth_module.login

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import com.teka.rufaa.data_layer.dtos.SignInDto
import com.teka.rufaa.data_layer.dtos.SignInResponseDto
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject

data class DemoUser(
    val name: String,
    val email: String,
    val password: String
)

data class LoginFormUiState(
    var email: String? = null,
    var password: String? = null,
    var selectedDemoUser: String? = null,
    val isSavingFormData: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null
)

data class LoginState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val appContext: Context,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _loginFormUiState = MutableStateFlow(LoginFormUiState())
    val loginFormUiState: StateFlow<LoginFormUiState> = _loginFormUiState

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors

    private val _loginState = mutableStateOf(LoginState())
    val loginState: State<LoginState> = _loginState

    private val _baseUrl = MutableStateFlow<String>("")
    val baseUrl: StateFlow<String> = _baseUrl

    // Demo users list
    val demoUsers = listOf(
        DemoUser("Japheth Kiprotich", "jkiprotich@intellisoftkenya.com", "123456"),
        DemoUser("Demo User 2", "demo2@example.com", "password123"),
        DemoUser("Test User", "test@example.com", "test123")
    )

    init {
        observeBaseUrl()
    }

    private fun observeBaseUrl() {
        viewModelScope.launch {
            val dataStoreRepository = DataStoreRepository(appContext)
            dataStoreRepository.getBaseUrl.collectLatest { url ->
                if (url.isEmpty()) {
                    dataStoreRepository.saveBaseUrl(AppEndpoints.DEFAULT_BASE_URL)
                }
                Timber.tag("BaseUrl").i(url)
                _baseUrl.value = url
            }
        }
    }

    fun onBaseUrlChange(url: String) {
        _baseUrl.value = url
    }

    fun changeBaseUrl(url: String) {
        viewModelScope.launch {
            dataStoreRepository.saveBaseUrl(url)
        }
    }

    fun selectDemoUser(userName: String) {
        val selectedUser = demoUsers.find { it.name == userName }
        if (selectedUser != null) {
            updateUiState {
                copy(
                    selectedDemoUser = userName,
                    email = selectedUser.email,
                    password = selectedUser.password
                )
            }
            clearAllFieldErrors()
        }
    }

    fun userSignIn() {
        _loginState.value = loginState.value.copy(isLoading = true)

        Timber.tag("LOGIN").i("Starting sign in process")
        viewModelScope.launch {
            try {
                val apiService = RetrofitProvider.simpleApiService(appContext)

                val signInDto = SignInDto(
                    email = loginFormUiState.value.email!!,
                    password = loginFormUiState.value.password!!
                )

                val response: Response<SignInResponseDto> =
                    apiService.submitSignInForm(
                        url = AppEndpoints.SIGN_IN, // This should be "user/signin"
                        body = signInDto
                    )

                Timber.tag("LoginVM").i("signInResponse: $response")

                if (response.isSuccessful) {
                    Timber.tag("LoginVM")
                        .i("successful signInResponseBody::: ${response.body()}")

                    val responseBody = response.body()
                    if (responseBody?.success == true) {
                        responseBody.data.let { userData ->
                            dataStoreRepository.saveLoggedInUserData(userData)
                            updateUiState {
                                copy(successMessage = responseBody.message ?: "Login Successful")
                            }
                        }
                    } else {
                        updateUiState {
                            copy(errorMessage = responseBody?.message ?: "Login Failed")
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    updateUiState {
                        copy(errorMessage = errorBody ?: "Login Failed")
                    }

                    when (response.code()) {
                        401 -> {
                            Timber.tag("API Call").i("Unauthorized: Invalid credentials")
                        }
                        503 -> {
                            Timber.tag("API Call")
                                .i("Service Unavailable: No internet connection")
                        }
                        else -> {
                            Timber.tag("API Call").e("HTTP error: ${response.code()}")
                        }
                    }
                }

                _loginState.value = loginState.value.copy(isLoading = false)

            } catch (e: Exception) {
                updateUiState { copy(errorMessage = "Login Failed: ${e.localizedMessage}") }
                Timber.tag("LoginVM").e("login failed: ${e.localizedMessage}")
                _loginState.value = loginState.value.copy(isLoading = false)
            }
        }
    }

    fun validateAndSubmitSimple(vararg fieldValidators: Pair<String?, (String) -> String?>): Boolean {
        val fieldNames = listOf("email", "password")
        val newErrors = mutableMapOf<String, String>()
        val errorMessages = mutableListOf<String>()

        fieldValidators.forEachIndexed { index, (value, validator) ->
            val error = validator(value ?: "")
            if (error != null) {
                errorMessages.add(error)
                if (index < fieldNames.size) {
                    newErrors[fieldNames[index]] = error
                }
            }
        }

        _fieldErrors.value = newErrors

        return if (errorMessages.isEmpty()) {
            userSignIn()
            true
        } else {
            updateUiState { copy(errorMessage = errorMessages.first()) }
            false
        }
    }

    fun updateUiState(update: LoginFormUiState.() -> LoginFormUiState) {
        _loginFormUiState.update { it.update() }
    }

    fun clearError() {
        updateUiState { copy(errorMessage = null) }
    }

    fun clearSuccess() {
        updateUiState { copy(successMessage = null) }
    }

    fun clearFieldError(fieldName: String) {
        _fieldErrors.value = _fieldErrors.value.toMutableMap().apply {
            remove(fieldName)
        }
    }

    fun clearAllFieldErrors() {
        _fieldErrors.value = emptyMap()
    }
}