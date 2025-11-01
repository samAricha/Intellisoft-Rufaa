package com.teka.rufaa.data_layer.api


import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface ApiService {

    @GET
    suspend fun genericGetRaw(
        @Url url: String,
        @QueryMap params: Map<String, String?>
    ): Response<ResponseBody>

    @POST
    suspend fun genericPostRaw(
        @Url url: String,
        @QueryMap params: Map<String, String?>
    ): Response<ResponseBody>


}
