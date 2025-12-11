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

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }
    }

    private fun handleNow(data: Map<String, String>) {
        val type = data["type"]
        if (type == "call") {
            val callerId = data["callerId"] ?: return
            val callerName = data["callerName"] ?: "Unknown"
            val callerPhone = data["callerPhone"] ?: ""
            val callId = data["callId"] ?: "" // Optional, might be used for tracking

            Log.d(TAG, "Received call FCM from: $callerName ($callerId)")

            // Start CallService as foreground service to show notification
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

