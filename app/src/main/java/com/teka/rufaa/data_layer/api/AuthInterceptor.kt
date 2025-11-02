package com.teka.rufaa.data_layer.api

import android.content.Context
import com.teka.rufaa.BuildConfig
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

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

object LoggingInterceptorProvider {
    val level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }


    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

}