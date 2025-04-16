package com.example.livechatdemo

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatApplication : Application()//{
//    override fun onCreate() {
//        super.onCreate()
//
//        val config = Configuration.Builder()
//            .setMinimumLoggingLevel(Log.DEBUG)
//            .build()
//
//        WorkManager.initialize(this, config)
//    }
//}