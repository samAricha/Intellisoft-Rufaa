package com.teka.rufaa.data_layer.persistence.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "vitals_table")
data class Vitals(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: String,
    
    @ColumnInfo(name = "visit_date")
    val visitDate: String,
    
    @ColumnInfo(name = "height")
    val height: String,
    
    @ColumnInfo(name = "weight")
    val weight: String,
    
    @ColumnInfo(name = "bmi")
    val bmi: String,
    
    @ColumnInfo(name = "bmi_category")
    val bmiCategory: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,
    
    @ColumnInfo(name = "sync_error")
    val syncError: String? = null
)