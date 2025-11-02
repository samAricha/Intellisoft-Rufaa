package com.teka.rufaa.data_layer.dtos

import kotlinx.serialization.Serializable


@Serializable
data class SignInResponseDto(
    val message: String,
    val success: Boolean,
    val code: Int,
    val data: UserData
)
