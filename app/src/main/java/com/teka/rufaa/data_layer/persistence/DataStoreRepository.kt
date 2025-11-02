package com.teka.rufaa.data_layer.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.teka.rufaa.data_layer.dtos.LoggedInUser
import com.teka.rufaa.data_layer.dtos.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

const val DS_REPOSITORY_TAG = "DS_REPOSITORY_TAG"


val Context.onBoardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "on_boarding_pref")
val Context.loggedInDataStore: DataStore<Preferences> by preferencesDataStore(name = "logged_in_pref")


class DataStoreRepository(context: Context) {

    private val onBoardingDataStore = context.onBoardingDataStore
    private val loggedInDataStore = context.loggedInDataStore

    private object PreferencesKey {
        val USER_TOKEN_KEY = stringPreferencesKey(name = "user_token")
        val onBoardingKey = booleanPreferencesKey(name = "on_boarding_completed")
        val isLoggedInKey = booleanPreferencesKey(name = "is_logged_in")
        val BASE_URL = stringPreferencesKey(name = "base_url")
    }

    private object UserPreferencesKey {
        val USER_ID = longPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("email")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val CREATED_AT = stringPreferencesKey("created_at")
        val UPDATED_AT = stringPreferencesKey("updated_at")
        val LOCAL_PASSWORD = stringPreferencesKey("local_password")
        val ROLE_ID = intPreferencesKey("role_id")
    }

    // Save logged in user data from new API
    suspend fun saveLoggedInUserData(userData: UserData) {
        loggedInDataStore.edit { preferences ->
            preferences[UserPreferencesKey.USER_ID] = userData.id.toLong()
            preferences[UserPreferencesKey.USER_NAME] = userData.name
            preferences[UserPreferencesKey.EMAIL] = userData.email
            preferences[UserPreferencesKey.ACCESS_TOKEN] = userData.access_token
            preferences[UserPreferencesKey.CREATED_AT] = userData.created_at
            preferences[UserPreferencesKey.UPDATED_AT] = userData.updated_at
            // Set logged in state to true when saving user data
            preferences[PreferencesKey.isLoggedInKey] = true
        }

        Timber.tag(DS_REPOSITORY_TAG).i("Saved user data: ${userData.name}, ID: ${userData.id}")
    }

    // Get logged in user data
    fun getLoggedInUserData(): Flow<UserData?> = loggedInDataStore.data.map { preferences ->
        val userId = preferences[UserPreferencesKey.USER_ID]
        if (userId != null) {
            UserData(
                id = userId.toInt(),
                name = preferences[UserPreferencesKey.USER_NAME] ?: "",
                email = preferences[UserPreferencesKey.EMAIL] ?: "",
                access_token = preferences[UserPreferencesKey.ACCESS_TOKEN] ?: "",
                created_at = preferences[UserPreferencesKey.CREATED_AT] ?: "",
                updated_at = preferences[UserPreferencesKey.UPDATED_AT] ?: ""
            )
        } else {
            null
        }
    }

    // Clear all user data (logout)
    suspend fun clearUserData() {
        loggedInDataStore.edit { preferences ->
            preferences.remove(UserPreferencesKey.USER_ID)
            preferences.remove(UserPreferencesKey.USER_NAME)
            preferences.remove(UserPreferencesKey.EMAIL)
            preferences.remove(UserPreferencesKey.ACCESS_TOKEN)
            preferences.remove(UserPreferencesKey.CREATED_AT)
            preferences.remove(UserPreferencesKey.UPDATED_AT)
            preferences.remove(UserPreferencesKey.LOCAL_PASSWORD)
            preferences.remove(UserPreferencesKey.ROLE_ID)
            preferences.remove(PreferencesKey.isLoggedInKey)
        }

        Timber.tag(DS_REPOSITORY_TAG).i("Cleared all user data")
    }

    // Check if user is logged in
    val isUserLoggedIn: Flow<Boolean> = loggedInDataStore.data
        .map { preferences ->
            val userId = preferences[UserPreferencesKey.USER_ID]
            val accessToken = preferences[UserPreferencesKey.ACCESS_TOKEN]
            val isLoggedIn = preferences[PreferencesKey.isLoggedInKey] ?: false

            // User is logged in if they have a valid user ID, access token, and logged in flag is true
            userId != null && accessToken != null && isLoggedIn
        }

    // Get access token
    val getAccessToken: Flow<String> = loggedInDataStore.data.map { preferences ->
        preferences[UserPreferencesKey.ACCESS_TOKEN] ?: ""
    }

    // Get user ID
    fun getUserId(): Flow<Int?> = loggedInDataStore.data.map { preferences ->
        preferences[UserPreferencesKey.USER_ID]?.toInt()
    }

    // Get user name
    fun getUserName(): Flow<String?> = loggedInDataStore.data.map { preferences ->
        preferences[UserPreferencesKey.USER_NAME]
    }

    // Get user email
    fun getUserEmail(): Flow<String?> = loggedInDataStore.data.map { preferences ->
        preferences[UserPreferencesKey.EMAIL]
    }

    // Legacy method support (if needed for other parts of the app)
    suspend fun saveUserData(userData: LoggedInUser) {
        loggedInDataStore.edit { preferences ->
            preferences[UserPreferencesKey.ROLE_ID] = userData.roles.first().id
        }
    }

    // Base URL management
    suspend fun saveBaseUrl(url: String) {
        loggedInDataStore.edit { preferences ->
            preferences[PreferencesKey.BASE_URL] = url
        }

        // Fetch saved data to confirm
        val savedData = loggedInDataStore.data.first()
        val savedUrl = savedData[PreferencesKey.BASE_URL]

        Timber.tag(DS_REPOSITORY_TAG).i("Confirmed saved base url: $savedUrl")
    }

    val getBaseUrl: Flow<String> = loggedInDataStore.data.map { preferences ->
        preferences[PreferencesKey.BASE_URL] ?: ""
    }

    // Role ID management
    val readLoggedInUserRoleId: Flow<Int> = loggedInDataStore.data.map { preferences ->
        preferences[UserPreferencesKey.ROLE_ID] ?: 0
    }

    suspend fun saveRoleId(roleId: Int) {
        loggedInDataStore.edit { preferences ->
            preferences[UserPreferencesKey.ROLE_ID] = roleId
        }
    }

    // Token management (legacy support)
    suspend fun saveToken(token: String) {
        loggedInDataStore.edit { preferences ->
            preferences[PreferencesKey.USER_TOKEN_KEY] = token
        }
    }

    // OnBoarding state management
    suspend fun saveOnBoardingState(completed: Boolean) {
        onBoardingDataStore.edit { preferences ->
            preferences[PreferencesKey.onBoardingKey] = completed
        }
    }

    fun readOnBoardingState(): Flow<Boolean> {
        return onBoardingDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val onBoardingState = preferences[PreferencesKey.onBoardingKey] ?: false
                onBoardingState
            }
    }

    // Logged in state management
    suspend fun saveLoggedInState(isLoggedIn: Boolean) {
        loggedInDataStore.edit { preferences ->
            preferences[PreferencesKey.isLoggedInKey] = isLoggedIn
        }
    }

    fun readLoggedInState(): Flow<Boolean> {
        return loggedInDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val isLoggedIn = preferences[PreferencesKey.isLoggedInKey] ?: false
                isLoggedIn
            }
    }

    // Helper: Get complete user profile info as a formatted string
    suspend fun getUserProfileInfo(): String {
        val preferences = loggedInDataStore.data.first()
        val name = preferences[UserPreferencesKey.USER_NAME] ?: "Unknown"
        val email = preferences[UserPreferencesKey.EMAIL] ?: "No email"
        val userId = preferences[UserPreferencesKey.USER_ID] ?: 0L

        return "Name: $name\nEmail: $email\nID: $userId"
    }

    // Helper: Check if access token exists
    suspend fun hasValidAccessToken(): Boolean {
        val preferences = loggedInDataStore.data.first()
        val token = preferences[UserPreferencesKey.ACCESS_TOKEN]
        return !token.isNullOrEmpty()
    }
}