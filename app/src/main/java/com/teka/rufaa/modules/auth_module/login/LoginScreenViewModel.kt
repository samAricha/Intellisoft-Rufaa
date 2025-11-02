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
import com.teka.rufaa.utils.converters.toParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject


data class LoginFormUiState(
    var role:  String? = null,
    var mobile: String? = null,
    var password: String? = null,
    val isSavingFormData: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val mobileError: String? = null,
    val passwordError: String? = null,
    val roleError: String? = null
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
    // UI state holder
    private val _loginFormUiState = MutableStateFlow(LoginFormUiState())
    val loginFormUiState: StateFlow<LoginFormUiState> = _loginFormUiState

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors

    private val _loginState = mutableStateOf(LoginState())
    val loginState: State<LoginState> = _loginState


    private val _baseUrl = MutableStateFlow<String>("")
    val baseUrl: StateFlow<String> = _baseUrl


    init {
        observeBaseUrl()
    }


    private fun observeBaseUrl() {
        viewModelScope.launch {
            val dataStoreRepository = DataStoreRepository(appContext)
            dataStoreRepository.getBaseUrl.collectLatest { url ->
                if (url.isEmpty()){
                    dataStoreRepository.saveBaseUrl(AppEndpoints.DEFAULT_BASE_URL)
                }
                Timber.tag("BaseUrl").i(url)
                _baseUrl.value = url

            }
        }
    }

    fun onBaseUrlChange(url: String){
        _baseUrl.value = url
    }

    fun changeBaseUrl(url: String){
        viewModelScope.launch{
            dataStoreRepository.saveBaseUrl(url)
        }
    }


    fun userSignIn(){
        _loginState.value = loginState.value.copy(isLoading = true)
        if (true) {
            Timber.tag("LOGIN").i("all fields are valid")
            viewModelScope.launch {
                try {
                    val apiService = RetrofitProvider.simpleApiService(appContext)

                    val queryParams = SignInDto(
                        mobile = loginFormUiState.value.mobile!!,
                        password = loginFormUiState.value.password!!,
                        category = loginFormUiState.value.role!!
                    ).toParams().toMap()

                    val response: Response<SignInResponseDto> =
                        apiService.submitSignInForm(
                            url = AppEndpoints.SIGN_IN,
                            params = queryParams
                        )

                    Timber.tag("LoginVM").i("signInResponse: $response")

                    if (response.isSuccessful) {
                        Timber.tag("LoginVM")
                            .i("successful signInResponseBody::: ${response.body()}")

                        val responseBody = response.body()
                        if (responseBody?.success == 1) {
                            responseBody.user_data.firstOrNull()?.let { userData ->
                                dataStoreRepository.saveLoggedInUserData(userData)
                                updateUiState { copy(successMessage = "Login Successful") }
                            }
                        }else if (responseBody?.success == 0){
                            updateUiState { copy(errorMessage = responseBody.status_desc) }
                        }

                    } else {
                        updateUiState { copy(errorMessage = "Login Failed") }

                        when (response.code()) {
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
                    updateUiState { copy(errorMessage = "Login Failed") }
                    Timber.tag("LoginVM").i("login failed: ${e.localizedMessage}")
                    _loginState.value = loginState.value.copy(isLoading = false)
                }

            }
        }else{
            Timber.tag("LOGIN").i("some fields are empty")
            _loginState.value = loginState.value.copy(isLoading = false)
        }
    }


    fun validateAndSubmitSimple(vararg fieldValidators: Pair<String?, (String) -> String?>): Boolean {
        val fieldNames = listOf("mobile", "password", "role")
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


    fun updateUiState(
        update: LoginFormUiState.() -> LoginFormUiState
    ) {
        _loginFormUiState.update { it.update() }
    }

    fun clearError() {
        updateUiState { copy(errorMessage = null)  }
    }

    fun clearSuccess() {
        updateUiState { copy(successMessage = null)  }
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