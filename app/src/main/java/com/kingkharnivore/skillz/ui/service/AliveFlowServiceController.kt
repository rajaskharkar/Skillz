package com.kingkharnivore.skillz.ui.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AliveFlowServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun start() {
        val intent = Intent(context, AliveFlowService::class.java)
        context.startForegroundService(intent)
    }

    fun stop() {
        context.stopService(Intent(context, AliveFlowService::class.java))
    }
}
