package com.teka.angaahewa.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

@Serializable
data class Supplier(
    val name: String,
    val id_number: String,
    val id: String? = null,
    val phone_number: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class Transporter(
    val name: String,
    val id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CollectionResponse(
    val id: String,
    val supplier_id: String,
    val supply_number: String,
    val weight_kg: Int,
    val transporter_id: Int,
    val field_agent_id: String,
    val collected_at: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val created_at: String,
    val updated_at: String,
    val suppliers: Supplier,
    val transporters: Transporter
)

@Serializable
data class CreateCollectionRequest(
    val supplier_id: String,
    val supply_number: String,
    val weight_kg: Int,
    val transporter_id: Int,
    val field_agent_id: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@OptIn(ExperimentalTime::class)
@Serializable
data class FieldAgent(
    val id: String,
    val user_id: String,
    val badge_id: String,
    val region: String,
    val notes: String? = null,
    val created_at: String,
    val updated_at: String
) {
    fun getCreatedAtInstant(): Instant = Instant.parse(created_at)
    fun getUpdatedAtInstant(): Instant = Instant.parse(updated_at)
}