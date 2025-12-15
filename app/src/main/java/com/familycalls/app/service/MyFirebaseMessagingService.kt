package com.familycalls.app.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Message Type: ${remoteMessage.messageType}")

        // Check if message contains notification payload (when app is in background)
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification payload received: ${notification.title} - ${notification.body}")
            // When notification payload exists, Android auto-shows it, but we still handle data
        }

        // Always handle data payload (or notification fallback)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        } else if (remoteMessage.notification != null) {
            // Fallback: if only notification payload exists, extract from it
            val notification = remoteMessage.notification!!
            val title = notification.title ?: "New Message"
            val body = notification.body ?: "You have a new message"
            
            Log.d(TAG, "Processing notification-only message: $title - $body")
            
            // Try to show notification with basic info
            // Note: We can't identify sender without data payload, but at least show something
            try {
                val notificationManager = MessageNotificationManager(applicationContext)
                // Use title as sender name if available
                notificationManager.showMessageNotification(
                    "unknown", // We don't have senderId from notification payload
                    title,
                    body
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification from notification payload", e)
            }
        }
    }

    private fun handleNow(data: Map<String, String>) {
        val type = data["type"]
        
        when (type) {
            "call", "incoming_call" -> {
                val callerId = data["callerId"] ?: return
                val callerName = data["callerName"] ?: "Unknown"
                val callerPhone = data["callerPhone"] ?: ""
                val callId = data["callId"] ?: "" // Optional, might be used for tracking

                Log.d(TAG, "Received call FCM from: $callerName ($callerId)")
                
                // Check if screen is on - only start activity directly if screen is OFF
                val powerManager = applicationContext.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                val isScreenOn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
                    powerManager.isInteractive
                } else {
                    @Suppress("DEPRECATION")
                    powerManager.isScreenOn
                }
                
                Log.d(TAG, "Screen is on: $isScreenOn")
                
                // Only start VideoCallActivity directly when screen is OFF (to wake it)
                // When screen is ON, only show notification pop-up (via CallService)
                if (!isScreenOn) {
                    Log.d(TAG, "Screen is OFF - starting VideoCallActivity directly to wake screen...")
                    try {
                        val activityIntent = android.content.Intent(applicationContext, com.familycalls.app.ui.call.VideoCallActivity::class.java).apply {
                            putExtra("contactId", callerId)
                            putExtra("contactName", callerName)
                            putExtra("contactPhone", callerPhone)
                            putExtra("isIncoming", true)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        }
                        
                        Log.d(TAG, "Starting activity with intent: $activityIntent")
                        applicationContext.startActivity(activityIntent)
                        Log.d(TAG, "✓ VideoCallActivity startActivity() called successfully")
                    } catch (e: android.content.ActivityNotFoundException) {
                        Log.e(TAG, "✗ ActivityNotFoundException: VideoCallActivity not found!", e)
                    } catch (e: android.os.DeadObjectException) {
                        Log.e(TAG, "✗ DeadObjectException: System killed app, activity start blocked", e)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "✗ SecurityException: Permission denied to start activity", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Failed to start activity directly", e)
                        e.printStackTrace()
                    }
                } else {
                    Log.d(TAG, "Screen is ON - will only show notification pop-up (no full-screen activity)")
                }

                // Also start CallService to show notification (for heads-up when screen is already on)
                val serviceIntent = android.content.Intent(applicationContext, CallService::class.java).apply {
                    action = CallService.ACTION_SHOW_INCOMING_CALL
                    putExtra("callerId", callerId)
                    putExtra("callerName", callerName)
                    putExtra("callerPhone", callerPhone)
                    putExtra("callId", callId)
                }

                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    Log.d(TAG, "CallService started from FCM")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start CallService from FCM", e)
                    // Fallback: show notification directly
                    CallNotificationManager(applicationContext).showIncomingCallNotification(
                        callerId,
                        callerName,
                        callerPhone
                    )
                }
            }
            "message" -> {
                val senderId = data["senderId"] ?: return
                val senderName = data["senderName"] ?: "Unknown"
                val messageText = data["messageText"] ?: data["body"] ?: "New message"

                Log.d(TAG, "=== MESSAGE NOTIFICATION ===")
                Log.d(TAG, "Sender ID: $senderId")
                Log.d(TAG, "Sender Name: $senderName")
                Log.d(TAG, "Message Text: $messageText")
                Log.d(TAG, "Full data payload: $data")

                // Show message notification
                try {
                    if (messageText.isEmpty() || messageText == "New message") {
                        Log.w(TAG, "⚠️ Message text is empty or default - checking data payload")
                        Log.w(TAG, "Available keys: ${data.keys.joinToString()}")
                    }
                    
                    val notificationManager = MessageNotificationManager(applicationContext)
                    notificationManager.showMessageNotification(
                        senderId,
                        senderName,
                        messageText
                    )
                    Log.d(TAG, "✓ Message notification shown from FCM")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Failed to show message notification from FCM", e)
                    e.printStackTrace()
                }
            }
            else -> {
                Log.w(TAG, "Unknown FCM message type: $type")
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // Get userId from SharedPreferences (we don't use Firebase Auth)
        val prefs = applicationContext.getSharedPreferences("FamilyCalls", android.content.Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: ""
        
        if (userId.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener { 
                    Log.d(TAG, "FCM Token updated for user: $userId")
                }
                .addOnFailureListener { e -> 
                    Log.e(TAG, "Error updating FCM token for user: $userId", e)
                }
        } else {
            Log.w(TAG, "Cannot update FCM token: userId is empty")
        }
    }
}

