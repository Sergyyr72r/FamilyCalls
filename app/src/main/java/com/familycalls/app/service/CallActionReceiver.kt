package com.familycalls.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.familycalls.app.ui.call.VideoCallActivity
import com.google.firebase.firestore.FirebaseFirestore

class CallActionReceiver : BroadcastReceiver() {
    
    private val db = FirebaseFirestore.getInstance()
    private val callsCollection = db.collection("calls")
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("CallActionReceiver", "=== onReceive called ===")
        Log.d("CallActionReceiver", "Action: ${intent.action}")
        Log.d("CallActionReceiver", "Extras: ${intent.extras?.keySet()}")
        
        val action = intent.action ?: run {
            Log.e("CallActionReceiver", "No action in intent!")
            return
        }
        
        val contactId = intent.getStringExtra("contactId") ?: run {
            Log.e("CallActionReceiver", "No contactId in intent!")
            return
        }
        val contactName = intent.getStringExtra("contactName") ?: ""
        val contactPhone = intent.getStringExtra("contactPhone") ?: ""
        
        Log.d("CallActionReceiver", "Processing action: $action for contact: $contactName ($contactId)")
        
        // Cancel the notification
        CallNotificationManager(context).cancelNotification()
        
        // Stop CallService if it's running (safe to call even if not running)
        context.stopService(Intent(context, CallService::class.java))
        
        when (action) {
            CallNotificationManager.ACTION_ACCEPT -> {
                Log.d("CallActionReceiver", "Call accepted: $contactName")
                
                // Update call status in Firestore
                updateCallStatus(context, contactId, "accepted")
                
                // Open VideoCallActivity and auto-accept (user already pressed Accept button)
                val callIntent = Intent(context, VideoCallActivity::class.java).apply {
                    putExtra("contactId", contactId)
                    putExtra("contactName", contactName)
                    putExtra("contactPhone", contactPhone)
                    putExtra("isIncoming", true)
                    putExtra("autoAccept", true) // Flag to auto-accept when opened
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(callIntent)
            }
            
            CallNotificationManager.ACTION_REJECT -> {
                Log.d("CallActionReceiver", "Call rejected: $contactName")
                
                // Close any VideoCallActivity that might be open (from full-screen notification)
                val closeIntent = Intent(context, VideoCallActivity::class.java).apply {
                    this.action = "com.familycalls.app.ACTION_CLOSE"
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(closeIntent)
                
                // Stop CallService to stop ringtone and vibration
                val serviceIntent = Intent(context, CallService::class.java)
                serviceIntent.action = CallService.ACTION_END_CALL
                context.startService(serviceIntent)
                
                // Update call status in Firestore (this will notify the caller)
                updateCallStatus(context, contactId, "rejected")
            }
        }
    }
    
    private fun updateCallStatus(context: Context, callerId: String, status: String) {
        // Get current user ID from SharedPreferences
        val prefs = context.getSharedPreferences("FamilyCalls", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", "") ?: return
        
        callsCollection
            .whereEqualTo("callerId", callerId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "ringing")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.reference?.update("status", status)
            }
            .addOnFailureListener { e ->
                Log.e("CallActionReceiver", "Failed to update call status", e)
            }
    }
}

