package com.familycalls.app.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

class CallService : Service() {
    
    private lateinit var notificationManager: CallNotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    
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
                
                // Acquire wake lock to wake device and keep screen on
                acquireWakeLock()
                
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
                    releaseWakeLock()
                }
            }
            
            ACTION_END_CALL -> {
                notificationManager.cancelNotification()
                releaseWakeLock()
                stopForeground(true)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // Try to use screen wake lock (deprecated but still works for turning on screen)
            // The full-screen intent and activity's setTurnScreenOn should handle screen wake,
            // but this helps ensure the device wakes up
            var wakeLockFlags = PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
            
            // On older Android versions, we can use SCREEN_BRIGHT_WAKE_LOCK
            // This is deprecated but still functional for ensuring screen turns on
            @Suppress("DEPRECATION")
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O_MR1) {
                try {
                    wakeLockFlags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE
                    Log.d("CallService", "Using SCREEN_BRIGHT_WAKE_LOCK (deprecated but needed)")
                } catch (e: Exception) {
                    Log.w("CallService", "SCREEN_BRIGHT_WAKE_LOCK not available, using PARTIAL", e)
                }
            }
            
            wakeLock = powerManager.newWakeLock(
                wakeLockFlags,
                "FamilyCalls::IncomingCallWakeLock"
            )
            
            // Hold for 60 seconds - enough time for user to answer
            wakeLock?.acquire(60000)
            Log.d("CallService", "Wake lock acquired (flags: $wakeLockFlags)")
            Log.d("CallService", "Full-screen intent should wake screen via activity's setTurnScreenOn")
        } catch (e: Exception) {
            Log.e("CallService", "Failed to acquire wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("CallService", "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("CallService", "Failed to release wake lock", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
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

