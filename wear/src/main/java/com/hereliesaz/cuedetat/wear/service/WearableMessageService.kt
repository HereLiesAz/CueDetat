package com.hereliesaz.cuedetat.wear.service

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearableMessageService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        val path = messageEvent.path
        Log.d("WearableMessageService", "Received message: $path")
        
        when (path) {
            "/trainer/start" -> {
                val intent = Intent(this, SensorService::class.java).apply {
                    putExtra(SensorService.EXTRA_NODE_ID, messageEvent.sourceNodeId)
                }
                startForegroundService(intent)
            }
            "/trainer/stop" -> {
                val intent = Intent(this, SensorService::class.java)
                stopService(intent)
            }
        }
    }
}
