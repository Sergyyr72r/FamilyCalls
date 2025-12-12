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
        // Changed channel ID again to force a completely fresh channel creation
        private const val CHANNEL_ID = "family_calls_channel_v3"
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
                    
                    // Enable sound - use ringtone for calls
                    val soundUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
                    setSound(soundUri, android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build())
                    
                    // Enable vibration with pattern
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500) // Ring pattern
                    
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
            putExtra("contactId", callerId)
            putExtra("contactName", callerName)
        }
        
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REJECT,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification with heads-up display (like WhatsApp)
        // This will appear as a popup at the bottom of the screen
        val soundUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
        
        android.util.Log.d("CallNotificationManager", "Creating notification with full-screen intent for: $callerName")
        android.util.Log.d("CallNotificationManager", "Content intent flags: ${contentPendingIntent.intentSender}")
        android.util.Log.d("CallNotificationManager", "Target activity: VideoCallActivity")
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_CALL) // Call category triggers call UI
            .setSmallIcon(android.R.drawable.ic_menu_call) // System call icon
            .setContentTitle("Incoming Video Call")
            .setContentText("$callerName is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX) // MAX priority for urgent calls
            .setContentIntent(contentPendingIntent) // Opens activity when notification body is tapped
            // Full-screen intent is required for lock screen support
            // The intent opens VideoCallActivity with isIncoming=true, which shows Accept/Reject buttons
            // It does NOT auto-accept - user must tap the Accept button
            // Set to true to wake screen even when device is locked/off
            .setFullScreenIntent(contentPendingIntent, true) // Required for lock screen, wakes screen
            .setOngoing(true) // Can't be dismissed easily
            .setAutoCancel(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500)) // Ring pattern
            .setSound(soundUri) // Use ringtone for calls
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
            .setTimeoutAfter(60000) // Auto-dismiss after 60 seconds if not answered
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

