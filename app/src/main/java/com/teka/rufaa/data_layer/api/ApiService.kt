package com.teka.rufaa.data_layer.api


import com.teka.rufaa.data_layer.dtos.SignInDto
import com.teka.rufaa.data_layer.dtos.SignInResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface ApiService {


    ///////// auth ////////
    @POST
    suspend fun submitSignInForm(
        @Url url: String,
        @retrofit2.http.Body body: SignInDto
    ): Response<SignInResponseDto>

    ///////// generic GET request ////////
    @GET
    suspend fun get(
        @Url url: String,
        @QueryMap queryParams: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    ///////// generic POST request ////////
    @POST
    suspend fun post(
        @Url url: String,
        @Body body: Any
    ): Response<ResponseBody>


}
