package com.teka.rufaa.data_layer.persistence.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "patient_table")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "firstname")
    val firstname: String,
    
    @ColumnInfo(name = "lastname")
    val lastname: String,
    
    @ColumnInfo(name = "unique_id")
    val uniqueId: String,
    
    @ColumnInfo(name = "dob")
    val dob: String,
    
    @ColumnInfo(name = "gender")
    val gender: String,
    
    @ColumnInfo(name = "reg_date")
    val regDate: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "server_proceed")
    val serverProceed: Int? = null,
    
    @ColumnInfo(name = "sync_error")
    val syncError: String? = null
)