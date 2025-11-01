package com.teka.rufaa.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RufaaApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}