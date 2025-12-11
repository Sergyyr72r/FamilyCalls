package com.familycalls.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CallService : Service() {
    
    private lateinit var notificationManager: CallNotificationManager
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = CallNotificationManager(this)
        Log.d("CallService", "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            ACTION_SHOW_INCOMING_CALL -> {
                val callerId = intent.getStringExtra("callerId") ?: return START_NOT_STICKY
                val callerName = intent.getStringExtra("callerName") ?: ""
                val callerPhone = intent.getStringExtra("callerPhone") ?: ""
                
                Log.d("CallService", "Showing incoming call notification: callerId=$callerId, callerName=$callerName")
                
                // Create the notification with buttons (this will be used for both display and foreground service)
                val notification = notificationManager.createIncomingCallNotification(
                    callerId,
                    callerName,
                    callerPhone
                )
                
                // Start as foreground service using the same notification
                try {
                    startForeground(NOTIFICATION_ID, notification)
                    Log.d("CallService", "Foreground service started successfully with incoming call notification")
                } catch (e: Exception) {
                    Log.e("CallService", "Failed to start foreground service", e)
                }
            }
            
            ACTION_END_CALL -> {
                notificationManager.cancelNotification()
                stopForeground(true)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    companion object {
        const val ACTION_SHOW_INCOMING_CALL = "com.familycalls.app.SHOW_INCOMING_CALL"
        const val ACTION_END_CALL = "com.familycalls.app.END_CALL"
        // Use the same notification ID as CallNotificationManager to avoid duplicates
        private const val NOTIFICATION_ID = CallNotificationManager.NOTIFICATION_ID
    }
}

