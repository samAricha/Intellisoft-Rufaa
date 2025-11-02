package com.teka.rufaa.data_layer.api

import android.content.Context
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import kotlinx.coroutines.flow.first
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber




val TAG = "GenericApiService"
enum class ApiMethodType { GET, POST }
class GenericApiService private constructor(
    private val appContext: Context,
    private val dataStoreRepository: DataStoreRepository
) {
    companion object {
        @Volatile
        private var INSTANCE: GenericApiService? = null

        fun getInstance(
            appContext: Context,
            dataStoreRepository: DataStoreRepository
        ): GenericApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GenericApiService(appContext, dataStoreRepository).also { INSTANCE = it }
            }
        }
    }

    suspend fun getSimpleApiService() = RetrofitProvider.simpleApiService(context = appContext)


    suspend fun makeRawApiCall(
        mapPayload: Map<String, String?>,
        endpoint: String,
        method: ApiMethodType = ApiMethodType.GET
    ): Response<ResponseBody> {

        val userData = dataStoreRepository.getLoggedInUserData().first()
        val enrichedPayload = mapPayload.toMutableMap().apply {
            put("branch_id", userData?.branch_id.toString())
            put("user_id", userData?.userID.toString())
        }

        Timber.tag(TAG).i("Making RAW ${method.name} API call to $endpoint with payload: $enrichedPayload")
        val apiService = RetrofitProvider.simpleApiService(context = appContext)
        return executeRawApiCall(apiService, endpoint, enrichedPayload, method)
    }

    private suspend fun executeRawApiCall(
        apiService: ApiService,
        endpoint: String,
        payloadMap: Map<String, String?>,
        method: ApiMethodType
    ): Response<ResponseBody> {
        return when (method) {
            ApiMethodType.GET -> apiService.genericGetRaw(endpoint, payloadMap)
            ApiMethodType.POST -> apiService.genericPostRaw(endpoint, payloadMap)
        }
    }


}