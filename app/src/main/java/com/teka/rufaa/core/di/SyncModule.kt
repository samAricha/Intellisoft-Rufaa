package com.teka.rufaa.core.di

import android.content.Context
import com.teka.rufaa.utils.offline_sync_utils.AutoSyncManager
import com.teka.rufaa.utils.offline_sync_utils.NetworkConnectivityMonitor
import com.teka.rufaa.utils.sync.DataSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    
    @Provides
    @Singleton
    fun provideAutoSyncManager(
        @ApplicationContext context: Context,
        networkMonitor: NetworkConnectivityMonitor,
        dataSyncService: DataSyncService
    ): AutoSyncManager {
        return AutoSyncManager(context, networkMonitor, dataSyncService)
    }
}