package com.teka.rufaa.data_layer.persistence.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * General Assessment Entity (Form A) - For BMI <= 25
 */
@Entity(tableName = "general_assessment_table")
data class GeneralAssessment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: String,
    
    @ColumnInfo(name = "visit_date")
    val visitDate: String,
    
    @ColumnInfo(name = "general_health")
    val generalHealth: String,
    
    @ColumnInfo(name = "on_diet_to_lose_weight")
    val onDietToLoseWeight: String,
    
    @ColumnInfo(name = "comments")
    val comments: String,
    
    @ColumnInfo(name = "form_type")
    val formType: String = "A",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,
    
    @ColumnInfo(name = "visit_id")
    val visitId: Int? = null,
    
    @ColumnInfo(name = "sync_error")
    val syncError: String? = null
)

/**
 * Overweight Assessment Entity (Form B) - For BMI > 25
 */
@Entity(tableName = "overweight_assessment_table")
data class OverweightAssessment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: String,
    
    @ColumnInfo(name = "visit_date")
    val visitDate: String,
    
    @ColumnInfo(name = "general_health")
    val generalHealth: String,
    
    @ColumnInfo(name = "currently_using_drugs")
    val currentlyUsingDrugs: String,
    
    @ColumnInfo(name = "comments")
    val comments: String,
    
    @ColumnInfo(name = "form_type")
    val formType: String = "B",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,
    
    @ColumnInfo(name = "visit_id")
    val visitId: Int? = null,
    
    @ColumnInfo(name = "sync_error")
    val syncError: String? = null
)