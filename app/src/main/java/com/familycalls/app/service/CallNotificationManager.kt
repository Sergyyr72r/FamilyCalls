package com.familycalls.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.familycalls.app.R
import com.familycalls.app.ui.call.VideoCallActivity

class CallNotificationManager(private val context: Context) {
    
    companion object {
        // Changed channel ID to force fresh channel creation with call sound/vibration settings
        private const val CHANNEL_ID = "family_calls_channel_v4_call_ring"
        const val NOTIFICATION_ID = 1001 // Made public so CallService can use it
        
        // Action IDs
        const val ACTION_ACCEPT = "com.familycalls.app.ACTION_ACCEPT_CALL"
        const val ACTION_REJECT = "com.familycalls.app.ACTION_REJECT_CALL"
        
        private const val REQUEST_CODE_ACCEPT = 100
        private const val REQUEST_CODE_REJECT = 101
        private const val REQUEST_CODE_FULL_SCREEN = 102
    }
    
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        try {
            createNotificationChannel()
        } catch (e: Exception) {
            android.util.Log.e("CallNotificationManager", "Error creating notification channel", e)
        }
    }
    
    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Recreating channel to ensure settings are correct
                // Note: System blocks repeated deletion/creation, so we only create if missing or update
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Family Calls",
                    NotificationManager.IMPORTANCE_HIGH // Required for heads-up notifications
                ).apply {
                    description = "Incoming video calls"
                    setShowBadge(true)
                    
                    // Enable sound - use standard ringtone for calls
                    val soundUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
                    setSound(soundUri, android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build())
                    
                    // Enable vibration with standard call pattern (long vibrations like phone calls)
                    enableVibration(true)
                    // Standard call vibration: long pause, long vibrate, short pause, long vibrate (repeats)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                    
                    // Enable lights
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                    
                    // Set bypass DND (Do Not Disturb) for calls
                    setBypassDnd(true)
                    
                    // Important: Set lockscreen visibility
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                
                // For Android 11+ (API 30+), we need to set canBubble and allow full-screen intents
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Allow full-screen intents for this channel
                    channel.setAllowBubbles(false) // Bubbles not needed
                }
                
                notificationManager.createNotificationChannel(channel)
                
                android.util.Log.d("CallNotificationManager", "Notification channel created/updated: $CHANNEL_ID")
            }
        } catch (e: Exception) {
            android.util.Log.e("CallNotificationManager", "Error in createNotificationChannel", e)
            throw e
        }
    }
    
    fun showIncomingCallNotification(
        callerId: String,
        callerName: String,
        callerPhone: String
    ) {
        android.util.Log.d("CallNotificationManager", "showIncomingCallNotification called: callerId=$callerId, callerName=$callerName")
        android.util.Log.d("CallNotificationManager", "Context: ${context.javaClass.simpleName}, package: ${context.packageName}")
        
        val notification = createIncomingCallNotification(callerId, callerName, callerPhone)
        
        android.util.Log.d("CallNotificationManager", "Posting notification with ID: $NOTIFICATION_ID")
        android.util.Log.d("CallNotificationManager", "Notification channel: $CHANNEL_ID")
        
        try {
            // Check if notifications are enabled
            val areNotificationsEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else {
                true
            }
            android.util.Log.d("CallNotificationManager", "Notifications enabled: $areNotificationsEnabled")
            
            if (!areNotificationsEnabled) {
                android.util.Log.w("CallNotificationManager", "Notifications are disabled for this app!")
            }
            
            notificationManager.notify(NOTIFICATION_ID, notification)
            android.util.Log.d("CallNotificationManager", "Notification posted successfully")
        } catch (e: Exception) {
            android.util.Log.e("CallNotificationManager", "Failed to post notification", e)
            e.printStackTrace()
        }
    }
    
    fun createIncomingCallNotification(
        callerId: String,
        callerName: String,
        callerPhone: String
    ): Notification {
        // Content intent (opens VideoCallActivity when notification body is tapped)
        // Note: We don't auto-open the activity - user must tap Accept button
        // Add flags to wake screen and show on locked screen
        val contentIntent = Intent(context, VideoCallActivity::class.java).apply {
            putExtra("contactId", callerId)
            putExtra("contactName", callerName)
            putExtra("contactPhone", callerPhone)
            putExtra("isIncoming", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_FULL_SCREEN,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Accept action - this is the ONLY way to accept the call
        val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = ACTION_ACCEPT
            setPackage(context.packageName) // Explicitly set package for better reliability
            putExtra("contactId", callerId)
            putExtra("contactName", callerName)
            putExtra("contactPhone", callerPhone)
        }
        
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ACCEPT,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Reject action
        val rejectIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = ACTION_REJECT
            setPackage(context.packageName) // Explicitly set package for better reliability
            putExtra("contactId", callerId)
            putExtra("contactName", callerName)
        }
        
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REJECT,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        android.util.Log.d("CallNotificationManager", "Created PendingIntents - Accept: $acceptPendingIntent, Reject: $rejectPendingIntent")
        
        // Build notification with heads-up display (like WhatsApp)
        // This will appear as a popup at the bottom of the screen
        val soundUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
        
        android.util.Log.d("CallNotificationManager", "Creating notification with full-screen intent for: $callerName")
        android.util.Log.d("CallNotificationManager", "Content intent flags: ${contentPendingIntent.intentSender}")
        android.util.Log.d("CallNotificationManager", "Target activity: VideoCallActivity")
        
        // Standard call vibration pattern: long pause, long vibrate, short pause, long vibrate
        // This mimics standard phone call vibration and will repeat as long as notification is active
        val callVibrationPattern = longArrayOf(0, 1000, 500, 1000)
        
        // Check if screen is on - only show full-screen notification when screen is off
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isScreenOn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
        
        android.util.Log.d("CallNotificationManager", "Screen is on: $isScreenOn - full-screen intent will be ${if (isScreenOn) "disabled" else "enabled"}")
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_CALL) // Call category triggers call UI
            .setSmallIcon(android.R.drawable.ic_menu_call) // System call icon
            .setContentTitle("Incoming Video Call")
            .setContentText("$callerName is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX) // MAX priority for urgent calls
            .setContentIntent(contentPendingIntent) // Opens activity when notification body is tapped
        
        // Full-screen intent: only enable when screen is OFF
        // When screen is ON, only show pop-up notification (heads-up)
        // When screen is OFF, full-screen notification will wake screen
        if (!isScreenOn) {
            notificationBuilder.setFullScreenIntent(contentPendingIntent, true) // Only when screen is off
            android.util.Log.d("CallNotificationManager", "Full-screen intent enabled (screen is off)")
        } else {
            android.util.Log.d("CallNotificationManager", "Full-screen intent disabled (screen is on - pop-up only)")
        }
        
        val notification = notificationBuilder
            .setOngoing(true) // Can't be dismissed easily - keeps sound/vibration going
            .setAutoCancel(false) // Don't auto-cancel - must be explicitly cancelled
            // Use standard call sound and vibration (sound is set on channel, vibration repeats while notification is active)
            .setSound(soundUri) // Standard ringtone sound (uses channel's audio attributes)
            .setVibrate(callVibrationPattern) // Standard call vibration pattern (repeats while notification is active)
            .setLights(android.graphics.Color.BLUE, 1000, 1000) // Blinking blue light
            .addAction(
                android.R.drawable.ic_menu_call, // Accept icon
                "Accept",
                acceptPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel, // Reject icon
                "Reject",
                rejectPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$callerName is calling you")
                    .setBigContentTitle("Incoming Video Call")
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            // No timeout - notification continues until answered or rejected (sound/vibration will repeat)
            .setShowWhen(false) // Don't show timestamp
            .build()
        
        android.util.Log.d("CallNotificationManager", "Notification built with full-screen intent")
        android.util.Log.d("CallNotificationManager", "Priority: MAX, Category: CALL, FullScreenIntent: true")
        
        return notification
    }
    
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    fun updateNotificationToOngoingCall(callerName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Ongoing Call")
            .setContentText("In call with $callerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
}

