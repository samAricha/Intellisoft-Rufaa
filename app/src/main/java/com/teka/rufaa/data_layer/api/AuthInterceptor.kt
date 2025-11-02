package com.teka.rufaa.data_layer.api

import android.content.Context
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val dataStoreRepository = DataStoreRepository(context)
        val token = runBlocking {
            dataStoreRepository.getAccessToken.first()
        }

        val request = chain.request().newBuilder()
        if (token.isNotEmpty()) {
            request.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(request.build())
    }
}