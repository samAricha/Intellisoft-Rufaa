package com.teka.rufaa.core.di

import android.content.Context
import androidx.room.Room
import com.teka.rufaa.data_layer.persistence.DataStoreRepository
import com.teka.rufaa.data_layer.persistence.room.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStoreRepository(
        @ApplicationContext context: Context
    ) = DataStoreRepository(context = context)

    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .build()
        Timber.tag("Hilt: AppDatabase").d("Database created: %s", database.isOpen)
        return database
    }


}