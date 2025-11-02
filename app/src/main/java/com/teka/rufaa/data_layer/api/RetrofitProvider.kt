package com.teka.rufaa.data_layer.api


import android.content.Context
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import timber.log.Timber
import kotlin.text.ifEmpty

const val RETROFIT_TAG = "RETROFIT_TAG"




object RetrofitProvider {

    suspend fun simpleApiService(context: Context): ApiService{
        Timber.tag(RETROFIT_TAG).i("creating simple api service")
        val dataStoreRepository = DataStoreRepository(context)
        val baseUrl = dataStoreRepository.getBaseUrl.first().ifEmpty { "https://heximas-avo.appspot.com/master/oil/" }

        Timber.tag(RETROFIT_TAG).i("base url: $baseUrl")
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkhttpClient(context))
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun provideOkhttpClient(context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .build()

}