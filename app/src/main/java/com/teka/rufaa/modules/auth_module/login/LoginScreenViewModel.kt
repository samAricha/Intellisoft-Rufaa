package com.teka.rufaa.modules.auth_module.login

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import com.teka.rufaa.data_layer.DataStoreRepository
import com.teka.rufaa.domain.FieldAgent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope


data class LoginFormUiState(
    var role:  String? = null,
    var email: String? = null,
    var password: String? = null,
    val isSavingFormData: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val emailError: String? = null,
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
    private val dataStoreRepository: DataStoreRepository,
    private val supabase: SupabaseClient
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
        clearAllFieldErrors()
    }



    @OptIn(DelicateCoroutinesApi::class)
    fun userSignIn(){
        _loginState.value = loginState.value.copy(isLoading = true)
        if (true) {
            Timber.tag("LOGIN").i("all fields are valid")
            viewModelScope.launch {
                try {
                    // Perform sign in
                    supabase.auth.signInWith(Email) {
                        email = loginFormUiState.value.email.toString()
                        password = loginFormUiState.value.password.toString()
                    }

                    GlobalScope.launch {
                        getFieldAgent()
                    }



                    // If successful, get the current session
                    val session = supabase.auth.currentSessionOrNull()
                    val user = supabase.auth.currentUserOrNull()

                    if (session != null && user != null) {
//                        getFieldAgent()
                        val accessToken = session.accessToken
                        val refreshToken = session.refreshToken
                        // Navigate to home/dashboard
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


    suspend fun getFieldAgent() {
            Timber.tag("getFieldAgent").i("Getting Field Agent")

            try {
                // Get current user ID
                val currentUser = supabase.auth.currentUserOrNull()
                val userId = currentUser?.id

                if (userId == null) {
                    Log.e("FieldAgent", "No authenticated user found")
                    // Handle unauthenticated state
                    return
                }

                // Call the Supabase function
                val response = supabase.postgrest.rpc(
                    function = "get_field_agent_by_user_id",
                    parameters = mapOf("p_user_id" to userId)
                )

                val fieldAgentList = response.decodeList<FieldAgent>()

                if (fieldAgentList.isNotEmpty()) {
                    val fieldAgent: FieldAgent = fieldAgentList.first()

                    // Save to DataStore
                    dataStoreRepository.saveFieldAgentData(fieldAgent)

                    // Update state
//                    _fieldAgent.value = fieldAgent

                    Log.d("FieldAgent", "Field agent found and saved: ${fieldAgent.badge_id}")
                } else {
                    // Clear any existing field agent data
                    dataStoreRepository.clearFieldAgentData()
//                    _fieldAgent.value = null
                    Log.d("FieldAgent", "No field agent found for user")
                }

            } catch (e: Exception) {
                Log.e("FieldAgent", "Error getting field agent: ${e.message}", e)
                // Handle error state
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