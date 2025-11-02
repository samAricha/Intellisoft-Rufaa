package com.teka.rufaa.data_layer.persistence.room

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(obj: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items:List<T>)

    @Update
    suspend fun update(obj: T)

    @Delete
    suspend fun delete(obj: T)

}

