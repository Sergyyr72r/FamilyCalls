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
        val action = intent.action
        val contactId = intent.getStringExtra("contactId") ?: return
        val contactName = intent.getStringExtra("contactName") ?: ""
        val contactPhone = intent.getStringExtra("contactPhone") ?: ""
        
        // Cancel the notification
        CallNotificationManager(context).cancelNotification()
        
        // Stop CallService if it's running (safe to call even if not running)
        context.stopService(Intent(context, CallService::class.java))
        
        when (action) {
            CallNotificationManager.ACTION_ACCEPT -> {
                Log.d("CallActionReceiver", "Call accepted: $contactName")
                
                // Update call status in Firestore
                updateCallStatus(context, contactId, "accepted")
                
                // Open VideoCallActivity
                val callIntent = Intent(context, VideoCallActivity::class.java).apply {
                    putExtra("contactId", contactId)
                    putExtra("contactName", contactName)
                    putExtra("contactPhone", contactPhone)
                    putExtra("isIncoming", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(callIntent)
            }
            
            CallNotificationManager.ACTION_REJECT -> {
                Log.d("CallActionReceiver", "Call rejected: $contactName")
                
                // Update call status in Firestore
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

