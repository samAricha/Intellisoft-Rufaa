package com.teka.rufaa.data_layer

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
import com.teka.rufaa.domain.FieldAgent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import kotlin.time.ExperimentalTime


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
        val BT_DEVICE_NAME = stringPreferencesKey(name = "bt_device_name")
        val BT_DEVICE_ADDRESS = stringPreferencesKey(name = "bt_device_address")
    }

    private object UserPreferencesKey {
        val USER_TOKEN_KEY = stringPreferencesKey("user_token")
        val ROLE_ID = intPreferencesKey("role_id")
        val LOCAL_PASSWORD = stringPreferencesKey("local_password")
        val BRANCH_ID = stringPreferencesKey("branch_id")
        val USER_ID = longPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val MOBILE = stringPreferencesKey("mobile")
        val CATEGORY = stringPreferencesKey("category")

    }


    private object FieldAgentPreferencesKey {
        val FIELD_AGENT_ID = stringPreferencesKey("field_agent_id")
        val FIELD_AGENT_USER_ID = stringPreferencesKey("field_agent_user_id")
        val FIELD_AGENT_BADGE_ID = stringPreferencesKey("field_agent_badge_id")
        val FIELD_AGENT_REGION = stringPreferencesKey("field_agent_region")
        val FIELD_AGENT_NOTES = stringPreferencesKey("field_agent_notes")
        val FIELD_AGENT_CREATED_AT = stringPreferencesKey("field_agent_created_at")
        val FIELD_AGENT_UPDATED_AT = stringPreferencesKey("field_agent_updated_at")
        val IS_FIELD_AGENT = booleanPreferencesKey("is_field_agent")
    }


    suspend fun saveLoggedInUserData(userData: UserData) {
        loggedInDataStore.edit { preferences ->
            preferences[UserPreferencesKey.USER_ID] = userData.userID
            preferences[UserPreferencesKey.USER_NAME] = userData.user_name
            preferences[UserPreferencesKey.MOBILE] = userData.mobile
            preferences[UserPreferencesKey.CATEGORY] = userData.category
            preferences[UserPreferencesKey.BRANCH_ID] = userData.branch_id.toString()
            preferences[UserPreferencesKey.LOCAL_PASSWORD] = userData.password
        }
    }

    suspend fun clearUserData() {
        loggedInDataStore.edit { preferences ->
            preferences.remove(UserPreferencesKey.USER_ID)
            preferences.remove(UserPreferencesKey.LOCAL_PASSWORD)
            preferences.remove(UserPreferencesKey.USER_NAME)
            preferences.remove(UserPreferencesKey.BRANCH_ID)
            preferences.remove(UserPreferencesKey.MOBILE)
            preferences.remove(UserPreferencesKey.CATEGORY)
            preferences.remove(UserPreferencesKey.ROLE_ID)

        }
    }


    @OptIn(ExperimentalTime::class)
    suspend fun saveFieldAgentData(fieldAgent: FieldAgent) {
        loggedInDataStore.edit { preferences ->
            preferences[FieldAgentPreferencesKey.FIELD_AGENT_ID] = fieldAgent.id
            preferences[FieldAgentPreferencesKey.FIELD_AGENT_USER_ID] = fieldAgent.user_id
            preferences[FieldAgentPreferencesKey.FIELD_AGENT_BADGE_ID] = fieldAgent.badge_id
            preferences[FieldAgentPreferencesKey.FIELD_AGENT_REGION] = fieldAgent.region
            preferences[FieldAgentPreferencesKey.FIELD_AGENT_NOTES] = fieldAgent.notes ?: ""
            preferences[FieldAgentPreferencesKey.FIELD_AGENT_CREATED_AT] = fieldAgent.created_at.toString()
            preferences[FieldAgentPreferencesKey.FIELD_AGENT_UPDATED_AT] = fieldAgent.updated_at.toString()
            preferences[FieldAgentPreferencesKey.IS_FIELD_AGENT] = true
        }

        Timber.tag(DS_REPOSITORY_TAG).i("Field agent data saved: Badge ID ${fieldAgent.badge_id}")
    }

    suspend fun clearFieldAgentData() {
        loggedInDataStore.edit { preferences ->
            preferences.remove(FieldAgentPreferencesKey.FIELD_AGENT_ID)
            preferences.remove(FieldAgentPreferencesKey.FIELD_AGENT_USER_ID)
            preferences.remove(FieldAgentPreferencesKey.FIELD_AGENT_BADGE_ID)
            preferences.remove(FieldAgentPreferencesKey.FIELD_AGENT_REGION)
            preferences.remove(FieldAgentPreferencesKey.FIELD_AGENT_NOTES)
            preferences.remove(FieldAgentPreferencesKey.FIELD_AGENT_CREATED_AT)
            preferences.remove(FieldAgentPreferencesKey.FIELD_AGENT_UPDATED_AT)
            preferences.remove(FieldAgentPreferencesKey.IS_FIELD_AGENT)
        }

        Timber.tag(DS_REPOSITORY_TAG).i("Field agent data cleared")
    }

    val isFieldAgent: Flow<Boolean> = loggedInDataStore.data
        .map { preferences ->
            preferences[FieldAgentPreferencesKey.IS_FIELD_AGENT] ?: false
        }


    @OptIn(ExperimentalTime::class)
    fun getFieldAgentData(): Flow<FieldAgent?> = loggedInDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isFieldAgent = preferences[FieldAgentPreferencesKey.IS_FIELD_AGENT] ?: false

            if (!isFieldAgent) {
                null
            } else {
                try {
                    FieldAgent(
                        id = preferences[FieldAgentPreferencesKey.FIELD_AGENT_ID] ?: "",
                        user_id = preferences[FieldAgentPreferencesKey.FIELD_AGENT_USER_ID] ?: "",
                        badge_id = preferences[FieldAgentPreferencesKey.FIELD_AGENT_BADGE_ID] ?: "",
                        region = preferences[FieldAgentPreferencesKey.FIELD_AGENT_REGION] ?: "",
                        notes = preferences[FieldAgentPreferencesKey.FIELD_AGENT_NOTES]?.takeIf { it.isNotEmpty() },
                        created_at = preferences[FieldAgentPreferencesKey.FIELD_AGENT_CREATED_AT] ?: "",
                        updated_at = preferences[FieldAgentPreferencesKey.FIELD_AGENT_UPDATED_AT] ?: ""
                    )
                } catch (e: Exception) {
                    Timber.tag(DS_REPOSITORY_TAG).e("Error parsing field agent data: ${e.message}")
                    null
                }
            }
        }



    val isUserLoggedIn: Flow<Boolean> = loggedInDataStore.data
        .map { preferences ->
            val userID = preferences[UserPreferencesKey.USER_ID]
            val branchID = preferences[UserPreferencesKey.BRANCH_ID]
            userID != null && branchID != null
        }

    fun getLoggedInUserData(): Flow<UserData?> = loggedInDataStore.data.map { preferences ->
        UserData(
            userID = preferences[UserPreferencesKey.USER_ID] ?: 0L,
            password = preferences[UserPreferencesKey.LOCAL_PASSWORD] ?: "",
            user_name = preferences[UserPreferencesKey.USER_NAME] ?: "",
            branch_id = preferences[UserPreferencesKey.BRANCH_ID]?.toLong() ?: 0L,
            mobile = preferences[UserPreferencesKey.MOBILE] ?: "",
            category = preferences[UserPreferencesKey.CATEGORY] ?: "",
            manager_id = "",
            loader_id = "",
            agent_id = "",
            staff_id = preferences[UserPreferencesKey.USER_ID] ?: 0L
        )
    }


    suspend fun saveUserData(userData: LoggedInUser) {
        loggedInDataStore.edit { preferences ->
//            preferences[UserPreferencesKey.ROLE_ID] = userData.roles.first().pivot.roleId
        }
    }



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









    suspend fun saveOnBoardingState(completed: Boolean) {
        onBoardingDataStore.edit { preferences ->
            preferences[PreferencesKey.onBoardingKey] = completed
        }
    }

    suspend fun saveLoggedInState(isLoggedIn: Boolean) {
        loggedInDataStore.edit { preferences ->
            preferences[PreferencesKey.isLoggedInKey] = isLoggedIn
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

}