package com.teka.rufaa.data_layer.api


import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import timber.log.Timber
import kotlin.text.ifEmpty

const val RETROFIT_TAG = "RETROFIT_TAG"


object RetrofitProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun simpleApiService(context: Context): ApiService{
        Timber.tag(RETROFIT_TAG).i("creating simple api service")
        val dataStoreRepository = DataStoreRepository(context)
        val baseUrl = dataStoreRepository.getBaseUrl.first().ifEmpty { "https://patientvisitapis.intellisoftkenya.com/api/" }

        Timber.tag(RETROFIT_TAG).i("base url: $baseUrl")
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkhttpClient(context))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun provideOkhttpClient(context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptorProvider.provideLoggingInterceptor())
            .addInterceptor(AuthInterceptor(context))
            .build()

}


////usage

/*
// GET request
val response = apiService.get(
    url = "users/profile",
    queryParams = mapOf("id" to "123")
)

// POST request
val response = apiService.post(
    url = "users/update",
    body = mapOf("name" to "John", "age" to 30)
)
*/

