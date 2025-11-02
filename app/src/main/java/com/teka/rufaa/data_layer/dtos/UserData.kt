package com.teka.rufaa.data_layer.dtos

import kotlinx.serialization.Serializable


@Serializable
data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val updated_at: String,
    val created_at: String,
    val access_token: String
)