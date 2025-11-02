package com.teka.rufaa.data_layer.persistence.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.teka.rufaa.data_layer.persistence.room.entities.GeneralAssessment
import com.teka.rufaa.data_layer.persistence.room.entities.OverweightAssessment
import com.teka.rufaa.data_layer.persistence.room.entities.Patient
import com.teka.rufaa.data_layer.persistence.room.entities.Vitals


@Database(
    entities = [
        Vitals::class,
        Patient::class,
        GeneralAssessment::class,
        OverweightAssessment::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters()
abstract class AppDatabase : RoomDatabase() {
    abstract fun vitalsDao(): VitalsDao
    abstract fun patientDao(): PatientDao
    abstract fun generalAssessmentDao(): GeneralAssessmentDao
    abstract fun overweightAssessmentDao(): OverweightAssessmentDao
}