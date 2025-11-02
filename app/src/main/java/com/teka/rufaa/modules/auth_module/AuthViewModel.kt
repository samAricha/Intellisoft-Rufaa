package com.teka.rufaa.modules.auth_module

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    val applicationContext: Context
    ) : ViewModel() {


    fun logout() {
        viewModelScope.launch {
            clearAuthToken()
            clearUserData()
            dataStoreRepository.clearUserData()
        }
    }

    suspend fun clearAuthToken() {
        dataStoreRepository.saveToken("")
    }

    suspend fun clearUserData() {
        dataStoreRepository.clearUserData()
    }
}
