package com.teka.rufaa.utils.sync

import android.content.Context
import com.teka.rufaa.data_layer.api.AppEndpoints
import com.teka.rufaa.data_layer.api.RetrofitProvider
import com.teka.rufaa.data_layer.persistence.room.GeneralAssessmentDao
import com.teka.rufaa.data_layer.persistence.room.OverweightAssessmentDao
import com.teka.rufaa.data_layer.persistence.room.PatientDao
import com.teka.rufaa.data_layer.persistence.room.VitalsDao
import com.teka.rufaa.modules.patient_registration.PatientRegistrationRequest
import com.teka.rufaa.modules.patient_registration.PatientRegistrationResponseDto
import com.teka.rufaa.modules.vitals.VitalsRequest
import com.teka.rufaa.modules.vitals.VitalsResponseDto
import com.teka.rufaa.modules.vitals.general_assesment.AssessmentResponseDto
import com.teka.rufaa.modules.vitals.general_assesment.GeneralAssessmentRequest
import com.teka.rufaa.modules.vitals.overweight_assesment.OverweightAssessmentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to handle background synchronization of unsynced data
 * Can be triggered manually or on network availability
 */
@Singleton
class DataSyncService @Inject constructor(
    private val appContext: Context,
    private val patientDao: PatientDao,
    private val vitalsDao: VitalsDao,
    private val generalAssessmentDao: GeneralAssessmentDao,
    private val overweightAssessmentDao: OverweightAssessmentDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Sync all unsynced data (patients, vitals, and assessments)
     */
    suspend fun syncAllData(): SyncResult {
        val patientResult = syncUnsyncedPatients()
        val vitalsResult = syncUnsyncedVitals()
        val generalAssessmentResult = syncUnsyncedGeneralAssessments()
        val overweightAssessmentResult = syncUnsyncedOverweightAssessments()

        return SyncResult(
            patientsSucceeded = patientResult.succeeded,
            patientsFailed = patientResult.failed,
            vitalsSucceeded = vitalsResult.succeeded,
            vitalsFailed = vitalsResult.failed,
            generalAssessmentsSucceeded = generalAssessmentResult.succeeded,
            generalAssessmentsFailed = generalAssessmentResult.failed,
            overweightAssessmentsSucceeded = overweightAssessmentResult.succeeded,
            overweightAssessmentsFailed = overweightAssessmentResult.failed
        )
    }

    /**
     * Sync unsynced patients
     */
    suspend fun syncUnsyncedPatients(): SyncItemResult {
        var succeeded = 0
        var failed = 0

        try {
            val unsyncedPatients = patientDao.getUnsyncedPatients()
            Timber.tag("DataSync").i("Syncing ${unsyncedPatients.size} unsynced patients")

            unsyncedPatients.forEach { patient ->
                try {
                    val apiService = RetrofitProvider.simpleApiService(appContext)

                    val request = PatientRegistrationRequest(
                        firstname = patient.firstname,
                        lastname = patient.lastname,
                        unique = patient.uniqueId,
                        dob = patient.dob,
                        gender = patient.gender,
                        reg_date = patient.regDate
                    )

                    val requestBody = json.encodeToString(
                        PatientRegistrationRequest.serializer(),
                        request
                    ).toRequestBody("application/json".toMediaType())

                    val response = apiService.post(
                        url = AppEndpoints.PATIENTS_REGISTER,
                        body = requestBody
                    )

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            val registrationResponse = json.decodeFromString<PatientRegistrationResponseDto>(responseBody)

                            if (registrationResponse.success) {
                                patientDao.markAsSynced(patient.id, registrationResponse.data.proceed)
                                succeeded++
                                Timber.tag("DataSync").i("Patient ${patient.uniqueId} synced successfully")
                            } else {
                                patientDao.updateSyncError(patient.id, registrationResponse.message)
                                failed++
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        patientDao.updateSyncError(patient.id, errorBody)
                        failed++
                    }
                } catch (e: Exception) {
                    patientDao.updateSyncError(patient.id, e.localizedMessage)
                    failed++
                    Timber.tag("DataSync").e("Error syncing patient ${patient.uniqueId}: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("DataSync").e("Error getting unsynced patients: ${e.localizedMessage}")
        }

        return SyncItemResult(succeeded, failed)
    }

    /**
     * Sync unsynced vitals
     */
    suspend fun syncUnsyncedVitals(): SyncItemResult {
        var succeeded = 0
        var failed = 0

        try {
            val unsyncedVitals = vitalsDao.getUnsyncedVitals()
            Timber.tag("DataSync").i("Syncing ${unsyncedVitals.size} unsynced vitals")

            unsyncedVitals.forEach { vitals ->
                try {
                    val apiService = RetrofitProvider.simpleApiService(appContext)

                    val request = VitalsRequest(
                        visit_date = vitals.visitDate,
                        height = vitals.height,
                        weight = vitals.weight,
                        bmi = vitals.bmi,
                        patient_id = vitals.patientId
                    )

                    val requestBody = json.encodeToString(
                        VitalsRequest.serializer(),
                        request
                    ).toRequestBody("application/json".toMediaType())

                    val response = apiService.post(
                        url = AppEndpoints.VITALS_ADD,
                        body = requestBody
                    )

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            val vitalsResponse = json.decodeFromString<VitalsResponseDto>(responseBody)

                            if (vitalsResponse.success) {
                                vitalsDao.markAsSynced(vitals.id, vitalsResponse.data.id)
                                succeeded++
                                Timber.tag("DataSync").i("Vitals for patient ${vitals.patientId} synced successfully")
                            } else {
                                vitalsDao.updateSyncError(vitals.id, vitalsResponse.message)
                                failed++
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        vitalsDao.updateSyncError(vitals.id, errorBody)
                        failed++
                    }
                } catch (e: Exception) {
                    vitalsDao.updateSyncError(vitals.id, e.localizedMessage)
                    failed++
                    Timber.tag("DataSync").e("Error syncing vitals for patient ${vitals.patientId}: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("DataSync").e("Error getting unsynced vitals: ${e.localizedMessage}")
        }

        return SyncItemResult(succeeded, failed)
    }

    /**
     * Sync unsynced general assessments
     */
    suspend fun syncUnsyncedGeneralAssessments(): SyncItemResult {
        var succeeded = 0
        var failed = 0

        try {
            val unsyncedAssessments = generalAssessmentDao.getUnsyncedAssessments()
            Timber.tag("DataSync").i("Syncing ${unsyncedAssessments.size} unsynced general assessments")

            unsyncedAssessments.forEach { assessment ->
                try {
                    val apiService = RetrofitProvider.simpleApiService(appContext)

                    val request = GeneralAssessmentRequest(
                        visit_date = assessment.visitDate,
                        general_health = assessment.generalHealth,
                        on_diet_to_lose_weight = assessment.onDietToLoseWeight,
                        comments = assessment.comments,
                        patient_id = assessment.patientId,
                        form_type = "A"
                    )

                    val requestBody = json.encodeToString(
                        GeneralAssessmentRequest.serializer(),
                        request
                    ).toRequestBody("application/json".toMediaType())

                    val response = apiService.post(
                        url = AppEndpoints.VISITS_ADD,
                        body = requestBody
                    )

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            val assessmentResponse = json.decodeFromString<AssessmentResponseDto>(responseBody)

                            if (assessmentResponse.success) {
                                generalAssessmentDao.markAsSynced(
                                    assessment.id,
                                    assessmentResponse.data.id,
                                    assessmentResponse.data.visit_id
                                )
                                succeeded++
                                Timber.tag("DataSync").i("General assessment for patient ${assessment.patientId} synced successfully")
                            } else {
                                generalAssessmentDao.updateSyncError(assessment.id, assessmentResponse.message)
                                failed++
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        generalAssessmentDao.updateSyncError(assessment.id, errorBody)
                        failed++
                    }
                } catch (e: Exception) {
                    generalAssessmentDao.updateSyncError(assessment.id, e.localizedMessage)
                    failed++
                    Timber.tag("DataSync").e("Error syncing general assessment for patient ${assessment.patientId}: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("DataSync").e("Error getting unsynced general assessments: ${e.localizedMessage}")
        }

        return SyncItemResult(succeeded, failed)
    }

    /**
     * Sync unsynced overweight assessments
     */
    suspend fun syncUnsyncedOverweightAssessments(): SyncItemResult {
        var succeeded = 0
        var failed = 0

        try {
            val unsyncedAssessments = overweightAssessmentDao.getUnsyncedAssessments()
            Timber.tag("DataSync").i("Syncing ${unsyncedAssessments.size} unsynced overweight assessments")

            unsyncedAssessments.forEach { assessment ->
                try {
                    val apiService = RetrofitProvider.simpleApiService(appContext)

                    val request = OverweightAssessmentRequest(
                        visit_date = assessment.visitDate,
                        general_health = assessment.generalHealth,
                        currently_using_drugs = assessment.currentlyUsingDrugs,
                        comments = assessment.comments,
                        patient_id = assessment.patientId,
                        form_type = "B"
                    )

                    val requestBody = json.encodeToString(
                        OverweightAssessmentRequest.serializer(),
                        request
                    ).toRequestBody("application/json".toMediaType())

                    val response = apiService.post(
                        url = AppEndpoints.VISITS_ADD,
                        body = requestBody
                    )

                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            val assessmentResponse = json.decodeFromString<AssessmentResponseDto>(responseBody)

                            if (assessmentResponse.success) {
                                overweightAssessmentDao.markAsSynced(
                                    assessment.id,
                                    assessmentResponse.data.id,
                                    assessmentResponse.data.visit_id
                                )
                                succeeded++
                                Timber.tag("DataSync").i("Overweight assessment for patient ${assessment.patientId} synced successfully")
                            } else {
                                overweightAssessmentDao.updateSyncError(assessment.id, assessmentResponse.message)
                                failed++
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        overweightAssessmentDao.updateSyncError(assessment.id, errorBody)
                        failed++
                    }
                } catch (e: Exception) {
                    overweightAssessmentDao.updateSyncError(assessment.id, e.localizedMessage)
                    failed++
                    Timber.tag("DataSync").e("Error syncing overweight assessment for patient ${assessment.patientId}: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            Timber.tag("DataSync").e("Error getting unsynced overweight assessments: ${e.localizedMessage}")
        }

        return SyncItemResult(succeeded, failed)
    }

    /**
     * Get count of unsynced items
     */
    suspend fun getUnsyncedCount(): UnsyncedCount {
        return UnsyncedCount(
            patients = patientDao.getUnsyncedCount(),
            vitals = vitalsDao.getUnsyncedCount(),
            generalAssessments = generalAssessmentDao.getUnsyncedCount(),
            overweightAssessments = overweightAssessmentDao.getUnsyncedCount()
        )
    }
}

data class SyncResult(
    val patientsSucceeded: Int,
    val patientsFailed: Int,
    val vitalsSucceeded: Int,
    val vitalsFailed: Int,
    val generalAssessmentsSucceeded: Int,
    val generalAssessmentsFailed: Int,
    val overweightAssessmentsSucceeded: Int,
    val overweightAssessmentsFailed: Int
) {
    val totalSucceeded: Int get() = patientsSucceeded + vitalsSucceeded +
            generalAssessmentsSucceeded + overweightAssessmentsSucceeded
    val totalFailed: Int get() = patientsFailed + vitalsFailed +
            generalAssessmentsFailed + overweightAssessmentsFailed
    val allSynced: Boolean get() = totalFailed == 0 && totalSucceeded > 0
}

data class SyncItemResult(
    val succeeded: Int,
    val failed: Int
)

data class UnsyncedCount(
    val patients: Int,
    val vitals: Int,
    val generalAssessments: Int,
    val overweightAssessments: Int
) {
    val total: Int get() = patients + vitals + generalAssessments + overweightAssessments
}