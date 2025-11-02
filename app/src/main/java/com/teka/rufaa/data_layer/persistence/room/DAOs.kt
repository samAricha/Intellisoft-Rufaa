package com.teka.rufaa.data_layer.persistence.room

import androidx.room.Dao
import androidx.room.Query
import com.teka.rufaa.data_layer.persistence.room.entities.GeneralAssessment
import com.teka.rufaa.data_layer.persistence.room.entities.OverweightAssessment
import com.teka.rufaa.data_layer.persistence.room.entities.Patient
import com.teka.rufaa.data_layer.persistence.room.entities.Vitals
import kotlinx.coroutines.flow.Flow


@Dao
interface VitalsDao : BaseDao<Vitals> {

    @Query("SELECT * FROM vitals_table ORDER BY created_at DESC")
    fun getAllVitals(): Flow<List<Vitals>>

    @Query("SELECT * FROM vitals_table WHERE id = :id LIMIT 1")
    suspend fun getVitalsById(id: Int): Vitals?

    @Query("SELECT * FROM vitals_table WHERE patient_id = :patientId ORDER BY created_at DESC")
    fun getVitalsByPatientId(patientId: String): Flow<List<Vitals>>

    @Query("SELECT * FROM vitals_table WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedVitals(): List<Vitals>

    @Query("UPDATE vitals_table SET is_synced = 1, server_id = :serverId WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int)

    @Query("UPDATE vitals_table SET sync_error = :error WHERE id = :localId")
    suspend fun updateSyncError(localId: Int, error: String?)

    @Query("DELETE FROM vitals_table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM vitals_table WHERE is_synced = 1")
    suspend fun deleteSyncedVitals()

    @Query("SELECT COUNT(*) FROM vitals_table WHERE is_synced = 0")
    suspend fun getUnsyncedCount(): Int
}


@Dao
interface PatientDao : BaseDao<Patient> {

    @Query("SELECT * FROM patient_table ORDER BY created_at DESC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patient_table WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Int): Patient?

    @Query("SELECT * FROM patient_table WHERE unique_id = :uniqueId LIMIT 1")
    suspend fun getPatientByUniqueId(uniqueId: String): Patient?

    @Query("SELECT * FROM patient_table WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedPatients(): List<Patient>

    @Query("UPDATE patient_table SET is_synced = 1, server_proceed = :serverProceed WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverProceed: Int)

    @Query("UPDATE patient_table SET sync_error = :error WHERE id = :localId")
    suspend fun updateSyncError(localId: Int, error: String?)

    @Query("DELETE FROM patient_table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM patient_table WHERE is_synced = 1")
    suspend fun deleteSyncedPatients()

    @Query("SELECT COUNT(*) FROM patient_table WHERE is_synced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT * FROM patient_table WHERE firstname LIKE '%' || :query || '%' OR lastname LIKE '%' || :query || '%' OR unique_id LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchPatients(query: String): Flow<List<Patient>>

    @Query("SELECT COUNT(*) FROM patient_table WHERE unique_id = :uniqueId")
    suspend fun checkIfUniqueIdExists(uniqueId: String): Int
}


@Dao
interface GeneralAssessmentDao : BaseDao<GeneralAssessment> {

    @Query("SELECT * FROM general_assessment_table ORDER BY created_at DESC")
    fun getAllAssessments(): Flow<List<GeneralAssessment>>

    @Query("SELECT * FROM general_assessment_table WHERE id = :id LIMIT 1")
    suspend fun getAssessmentById(id: Int): GeneralAssessment?

    @Query("SELECT * FROM general_assessment_table WHERE patient_id = :patientId ORDER BY created_at DESC")
    fun getAssessmentsByPatientId(patientId: String): Flow<List<GeneralAssessment>>

    @Query("SELECT * FROM general_assessment_table WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedAssessments(): List<GeneralAssessment>

    @Query("UPDATE general_assessment_table SET is_synced = 1, server_id = :serverId, visit_id = :visitId WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int, visitId: Int)

    @Query("UPDATE general_assessment_table SET sync_error = :error WHERE id = :localId")
    suspend fun updateSyncError(localId: Int, error: String?)

    @Query("DELETE FROM general_assessment_table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM general_assessment_table WHERE is_synced = 1")
    suspend fun deleteSyncedAssessments()

    @Query("SELECT COUNT(*) FROM general_assessment_table WHERE is_synced = 0")
    suspend fun getUnsyncedCount(): Int
}

@Dao
interface OverweightAssessmentDao : BaseDao<OverweightAssessment> {

    @Query("SELECT * FROM overweight_assessment_table ORDER BY created_at DESC")
    fun getAllAssessments(): Flow<List<OverweightAssessment>>

    @Query("SELECT * FROM overweight_assessment_table WHERE id = :id LIMIT 1")
    suspend fun getAssessmentById(id: Int): OverweightAssessment?

    @Query("SELECT * FROM overweight_assessment_table WHERE patient_id = :patientId ORDER BY created_at DESC")
    fun getAssessmentsByPatientId(patientId: String): Flow<List<OverweightAssessment>>

    @Query("SELECT * FROM overweight_assessment_table WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsyncedAssessments(): List<OverweightAssessment>

    @Query("UPDATE overweight_assessment_table SET is_synced = 1, server_id = :serverId, visit_id = :visitId WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int, visitId: Int)

    @Query("UPDATE overweight_assessment_table SET sync_error = :error WHERE id = :localId")
    suspend fun updateSyncError(localId: Int, error: String?)

    @Query("DELETE FROM overweight_assessment_table WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM overweight_assessment_table WHERE is_synced = 1")
    suspend fun deleteSyncedAssessments()

    @Query("SELECT COUNT(*) FROM overweight_assessment_table WHERE is_synced = 0")
    suspend fun getUnsyncedCount(): Int
}