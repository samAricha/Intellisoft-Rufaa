package com.teka.rufaa.data_layer.dtos

data class SignInResponseDto(
    val success: Int,
    val user_data: List<UserData>,
    val status_code: Int,
    val status_desc: String
)
