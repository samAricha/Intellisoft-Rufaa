package com.teka.rufaa.data_layer.dtos

import kotlinx.serialization.Serializable


@Serializable
data class SignInDto(
    val email: String,
    val password: String
)
