package com.teka.angaahewa.data_layer

data class UserData(
    val userID: Long,
    val password: String,
    val user_name: String,
    val manager_id: String?,
    val loader_id: String?,
    val agent_id: String?,
    val staff_id: Long,
    val branch_id: Long,
    val mobile: String,
    val category: String
)