package com.teka.angaahewa.data_layer

data class LoggedInUser(
    val id: Int,
    val name: String,
    val email: String?,
    val phone: String,
    val created_at: String,
    val updated_at: String,
    val roles: List<UserRoleDto>
)