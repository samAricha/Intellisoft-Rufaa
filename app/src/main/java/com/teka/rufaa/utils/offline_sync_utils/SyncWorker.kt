package com.teka.rufaa.utils.offline_sync_utils

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teka.rufaa.utils.sync.DataSyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker for syncing data with backend
 * Runs when network is available
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataSyncService: DataSyncService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.tag("SyncWorker").i("Starting background sync")
            
            // Check if there's unsynced data
            val unsyncedCount = dataSyncService.getUnsyncedCount()
            
            if (unsyncedCount.total == 0) {
                Timber.tag("SyncWorker").i("No unsynced data found")
                return Result.success()
            }

            Timber.tag("SyncWorker").i("Found ${unsyncedCount.total} unsynced items")
            
            // Perform sync
            val result = dataSyncService.syncAllData()
            
            Timber.tag("SyncWorker").i("""
                Sync completed:
                - Total succeeded: ${result.totalSucceeded}
                - Total failed: ${result.totalFailed}
            """.trimIndent())
            
            // If some items failed, retry
            if (result.totalFailed > 0) {
                Timber.tag("SyncWorker").w("${result.totalFailed} items failed to sync, will retry")
                Result.retry()
            } else {
                Result.success()
            }
            
        } catch (e: Exception) {
            Timber.tag("SyncWorker").e("Sync worker failed: ${e.localizedMessage}")
            Result.retry()
        }
    }
}