package com.teka.rufaa.utils.offline_sync_utils

import android.content.Context
import androidx.work.*
import com.teka.rufaa.utils.sync.DataSyncService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic synchronization when network becomes available
 */
@Singleton
class AutoSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val dataSyncService: DataSyncService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false

    /**
     * Start monitoring network and auto-sync when connected
     */
    fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        Timber.tag("AutoSync").i("Started monitoring network for auto-sync")

        scope.launch {
            var wasDisconnected = false

            networkMonitor.isConnected.collectLatest { isConnected ->
                Timber.tag("AutoSync").i("Network status changed: isConnected=$isConnected")

                if (isConnected && wasDisconnected) {
                    // Network just became available after being disconnected
                    Timber.tag("AutoSync").i("Network reconnected - triggering sync")
                    triggerSync()
                }

                wasDisconnected = !isConnected
            }
        }
    }

    /**
     * Trigger immediate sync
     */
    private suspend fun triggerSync() {
        try {
            // Check if there's actually unsynced data
            val unsyncedCount = dataSyncService.getUnsyncedCount()

            if (unsyncedCount.total == 0) {
                Timber.tag("AutoSync").i("No unsynced data to sync")
                return
            }

            Timber.tag("AutoSync").i("Starting sync of ${unsyncedCount.total} unsynced items")

            val result = dataSyncService.syncAllData()

            Timber.tag("AutoSync").i("""
                Sync completed:
                - Patients: ${result.patientsSucceeded} succeeded, ${result.patientsFailed} failed
                - Vitals: ${result.vitalsSucceeded} succeeded, ${result.vitalsFailed} failed
                - General Assessments: ${result.generalAssessmentsSucceeded} succeeded, ${result.generalAssessmentsFailed} failed
                - Overweight Assessments: ${result.overweightAssessmentsSucceeded} succeeded, ${result.overweightAssessmentsFailed} failed
                Total: ${result.totalSucceeded} succeeded, ${result.totalFailed} failed
            """.trimIndent())

        } catch (e: Exception) {
            Timber.tag("AutoSync").e("Error during auto-sync: ${e.localizedMessage}")
        }
    }

    /**
     * Schedule periodic sync using WorkManager (backup strategy)
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED) // Fixed: Use WorkManager's NetworkType
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15, // Minimum interval is 15 minutes
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("data_sync")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_data_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        Timber.tag("AutoSync").i("Scheduled periodic sync with WorkManager")
    }

    /**
     * Trigger one-time sync immediately (for manual sync buttons)
     */
    fun triggerManualSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED) // Fixed: Use WorkManager's NetworkType
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("manual_sync")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "manual_data_sync",
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )

        Timber.tag("AutoSync").i("Triggered manual sync")
    }

    /**
     * Cancel all scheduled syncs
     */
    fun cancelSync() {
        WorkManager.getInstance(context).cancelAllWorkByTag("data_sync")
        Timber.tag("AutoSync").i("Cancelled all scheduled syncs")
    }

    /**
     * Check sync status
     */
    suspend fun getSyncStatus(): SyncStatus {
        val unsyncedCount = dataSyncService.getUnsyncedCount()
        val isConnected = networkMonitor.isCurrentlyConnected()

        return SyncStatus(
            isConnected = isConnected,
            unsyncedCount = unsyncedCount.total,
            networkType = networkMonitor.getNetworkType()
        )
    }
}

data class SyncStatus(
    val isConnected: Boolean,
    val unsyncedCount: Int,
    val networkType: com.teka.rufaa.utils.offline_sync_utils.NetworkType // Changed to use your custom NetworkType
)