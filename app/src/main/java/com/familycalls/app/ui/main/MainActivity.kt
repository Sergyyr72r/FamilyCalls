package com.familycalls.app.ui.main

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familycalls.app.R
import com.familycalls.app.data.model.User
import com.familycalls.app.data.repository.AuthRepository
import com.familycalls.app.databinding.ActivityMainBinding
import com.familycalls.app.databinding.ItemContactBinding
import android.util.Log
import com.familycalls.app.service.CallNotificationManager
import com.familycalls.app.service.CallService
import com.familycalls.app.ui.chat.ChatActivity
import com.familycalls.app.ui.call.VideoCallActivity
import com.familycalls.app.utils.ShareUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private lateinit var prefs: SharedPreferences
    private var currentUserId: String = ""
    private val contactsAdapter = ContactsAdapter { contact ->
        showContactOptions(contact)
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }
    
    private fun isActivityDestroyed(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isDestroyed
        } else {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission for Android 13+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Check if notifications are enabled in system settings (defer to avoid crash)
                    binding.root.post {
                        checkNotificationSettings()
                    }
                }
            } else {
                // Check if notifications are enabled in system settings (defer to avoid crash)
                binding.root.post {
                    checkNotificationSettings()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking notification permissions", e)
        }

        try {
            prefs = getSharedPreferences("FamilyCalls", MODE_PRIVATE)
            currentUserId = prefs.getString("userId", "") ?: ""
            
            // Critical: Ensure we have a valid user ID
            if (currentUserId.isEmpty()) {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    currentUserId = firebaseUser.uid
                    prefs.edit().putString("userId", currentUserId).apply()
                    android.util.Log.d("MainActivity", "Recovered userId from Firebase Auth: $currentUserId")
                } else {
                    android.util.Log.w("MainActivity", "No user logged in, redirecting to AuthActivity")
                    // Clear any partial state
                    prefs.edit().clear().apply()
                    startActivity(Intent(this, com.familycalls.app.ui.auth.AuthActivity::class.java))
                    finish()
                    return
                }
            }
            
            android.util.Log.d("MainActivity", "MainActivity onCreate - currentUserId: $currentUserId")
            
            // Register FCM token for push notifications
            registerFCMToken()
            
            binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewContacts.adapter = contactsAdapter
            
            android.util.Log.d("MainActivity", "RecyclerView setup complete")
            
            loadContacts()
            android.util.Log.d("MainActivity", "loadContacts() called")
            
            // Listen for new users
            try {
                listenForUsers()
                android.util.Log.d("MainActivity", "listenForUsers() called")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in listenForUsers()", e)
            }
            
            // Listen for incoming calls
            try {
                listenForIncomingCalls()
                android.util.Log.d("MainActivity", "listenForIncomingCalls() called")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in listenForIncomingCalls()", e)
            }
            
            // Setup overflow menu
            try {
                val ivMoreOptions = findViewById<android.view.View>(R.id.ivMoreOptions)
                if (ivMoreOptions != null) {
                    ivMoreOptions.setOnClickListener { showMenu(it) }
                    android.util.Log.d("MainActivity", "Menu button setup complete")
                } else {
                    android.util.Log.w("MainActivity", "ivMoreOptions view not found in layout")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error setting up menu button", e)
            }
            
            android.util.Log.d("MainActivity", "onCreate() completed successfully")
            if (currentUserId.isEmpty()) {
                android.util.Log.w("MainActivity", "WARNING: currentUserId is empty! Listener won't work.")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "FATAL ERROR in onCreate()", e)
            e.printStackTrace()
            // Don't rethrow - let the activity continue if possible
        }
    }
    
    private fun showMenu(v: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, v)
        popup.menu.add("Simulate Incoming Call")
        popup.menu.add("Share App")
        popup.menu.add("Logout")
        
        popup.setOnMenuItemClickListener { item ->
            when(item.title) {
                "Simulate Incoming Call" -> {
                    simulateIncomingCall()
                    true
                }
                "Share App" -> {
                    shareDownloadLink()
                    true
                }
                "Logout" -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun logout() {
        // Clear local preferences
        prefs.edit().clear().apply()
        
        // Sign out from Firebase
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        
        // Return to AuthActivity
        val intent = Intent(this, com.familycalls.app.ui.auth.AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun registerFCMToken() {
        if (currentUserId.isEmpty()) {
            android.util.Log.w("MainActivity", "Cannot register FCM token: userId is empty")
            return
        }
        
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.w("MainActivity", "Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }
            
            val token = task.result
            android.util.Log.d("MainActivity", "FCM Token obtained: ${token.take(20)}...")
            
            // Save token to Firestore
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(currentUserId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    android.util.Log.d("MainActivity", "FCM Token registered successfully for user: $currentUserId")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("MainActivity", "Failed to register FCM token", e)
                }
        }
    }

    private fun simulateIncomingCall() {
        android.util.Log.d("MainActivity", "Simulating incoming call...")
        val serviceIntent = Intent(this, CallService::class.java).apply {
            action = CallService.ACTION_SHOW_INCOMING_CALL
            putExtra("callerId", "test_caller_id")
            putExtra("callerName", "Test Caller")
            putExtra("callerPhone", "+1 234 567 890")
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            android.widget.Toast.makeText(this, "Simulating call...", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start call simulation", e)
            android.widget.Toast.makeText(this, "Failed to simulate call: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "MainActivity onResume - listener should be active")
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("MainActivity", "MainActivity onPause - listener may not work in background")
    }
    
    private fun checkNotificationSettings() {
        try {
            if (isFinishing || isActivityDestroyed()) {
                android.util.Log.d("MainActivity", "Activity is finishing/destroyed, skipping notification check")
                return
            }
            
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            
            // Check if app-level notifications are enabled
            val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
            
            // Check if the specific channel is blocked
            val channelId = "family_calls_channel_v3"
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.getNotificationChannel(channelId)
            } else {
                null
            }
            
            val isChannelBlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channel != null) {
                channel.importance == android.app.NotificationManager.IMPORTANCE_NONE
            } else {
                false
            }

            if (!areNotificationsEnabled || isChannelBlocked) {
                if (!isFinishing && !isActivityDestroyed()) {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Notifications Disabled")
                        .setMessage("To receive calls, please enable notifications for Family Calls.")
                        .setPositiveButton("Settings") { _, _ ->
                            try {
                                val intent = Intent().apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (isChannelBlocked) {
                                            action = android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                                            putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
                                        } else {
                                            action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                                        }
                                    } else {
                                        action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                        putExtra("app_package", packageName)
                                        putExtra("app_uid", applicationInfo.uid)
                                    }
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Error opening settings", e)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking notification settings", e)
        }
    }

    private fun shareDownloadLink() {
        val downloadLink = ShareUtils.generateDownloadLink(packageName)
        ShareUtils.shareDownloadLink(this, downloadLink)
    }
    
    private fun loadContacts() {
        lifecycleScope.launch {
            val allUsers = authRepository.getAllUsers()
            val otherUsers = allUsers.filter { it.id != currentUserId }
            contactsAdapter.submitList(otherUsers)
            
            if (otherUsers.isEmpty()) {
                binding.tvNoContacts.visibility = View.VISIBLE
                binding.recyclerViewContacts.visibility = View.GONE
            } else {
                binding.tvNoContacts.visibility = View.GONE
                binding.recyclerViewContacts.visibility = View.VISIBLE
            }
        }
    }
    
    private fun listenForUsers() {
        FirebaseFirestore.getInstance().collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    loadContacts()
                }
            }
    }
    
    private val processedCallIds = mutableSetOf<String>() // Track processed calls to avoid duplicates
    
    private fun listenForIncomingCalls() {
        try {
            if (currentUserId.isEmpty()) {
                android.util.Log.w("MainActivity", "Cannot listen for calls: currentUserId is empty")
                return
            }
            
            if (isFinishing || isActivityDestroyed()) {
                android.util.Log.w("MainActivity", "Activity is finishing, skipping call listener setup")
                return
            }
            
            android.util.Log.d("MainActivity", "Setting up incoming call listener for userId: $currentUserId")
            
            val db = FirebaseFirestore.getInstance()
            val callsCollection = db.collection("calls")
            
            // Log the query we're setting up
            android.util.Log.d("MainActivity", "Query: calls where receiverId=$currentUserId AND status=ringing")
            
            // Store listener reference to prevent garbage collection
            val listener = callsCollection
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "ringing")
                .addSnapshotListener { snapshot, error ->
                    android.util.Log.d("MainActivity", "=== LISTENER CALLBACK TRIGGERED ===")
                    android.util.Log.d("MainActivity", "Has error: ${error != null}")
                    if (error != null) {
                        android.util.Log.e("MainActivity", "Listener error details: ${error.message}", error)
                    }
                    android.util.Log.d("MainActivity", "Has snapshot: ${snapshot != null}")
                    if (snapshot != null) {
                        android.util.Log.d("MainActivity", "Snapshot size: ${snapshot.documents.size}")
                        android.util.Log.d("MainActivity", "Snapshot metadata from cache: ${snapshot.metadata.isFromCache}")
                    }
                    
                    try {
                        if (error != null) {
                            android.util.Log.e("MainActivity", "Error listening for calls", error)
                            return@addSnapshotListener
                        }
                        
                        if (snapshot == null) {
                            android.util.Log.d("MainActivity", "Snapshot is null")
                            return@addSnapshotListener
                        }
                        
                        android.util.Log.d("MainActivity", "Call listener triggered. Documents: ${snapshot.documents.size}")
                        
                        // Use application context for notifications so they work even when activity is in background
                        val notificationManager = try {
                            CallNotificationManager(applicationContext)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to create CallNotificationManager", e)
                            return@addSnapshotListener
                        }
                        
                        // Process all ringing calls
                        snapshot.documents.forEach { document ->
                            try {
                                val callId = document.id
                                val callerId = document.getString("callerId")
                                val status = document.getString("status") ?: return@forEach
                                
                                android.util.Log.d("MainActivity", "Found call: id=$callId, callerId=$callerId, status=$status")
                                
                                if (callerId.isNullOrEmpty()) {
                                    android.util.Log.e("MainActivity", "Skipping call with missing callerId: $callId")
                                    return@forEach
                                }
                                
                                // Skip if we've already processed this call
                                if (processedCallIds.contains(callId)) {
                                    android.util.Log.d("MainActivity", "Skipping already processed call: $callId")
                                    return@forEach
                                }
                                
                                // Only process calls that are currently ringing
                                if (status == "ringing") {
                                    // Mark as processed immediately to prevent duplicates
                                    processedCallIds.add(callId)
                                    
                                    android.util.Log.d("MainActivity", "Processing incoming call from: $callerId")
                                    
                                    // Validate callerId before creating document reference
                                    if (callerId.isEmpty()) {
                                        android.util.Log.e("MainActivity", "Invalid empty callerId for call: $callId")
                                        return@forEach
                                    }

                                    // Get caller information
                                    db.collection("users").document(callerId).get()
                                        .addOnSuccessListener { userDoc ->
                                            try {
                                                if (!userDoc.exists()) {
                                                    android.util.Log.w("MainActivity", "Caller user document not found: $callerId")
                                                    processedCallIds.remove(callId) // Allow retry
                                                    return@addOnSuccessListener
                                                }
                                                
                                                val callerName = userDoc.getString("name") ?: "Unknown"
                                                val callerPhone = userDoc.getString("phone") ?: ""
                                                
                                                android.util.Log.d("MainActivity", "Caller info: name=$callerName, phone=$callerPhone")
                                                
                                                // Show notification directly - works even when activity is in background
                                                // Using application context ensures it works regardless of activity state
                                                notificationManager.showIncomingCallNotification(
                                                    callerId,
                                                    callerName,
                                                    callerPhone
                                                )
                                                android.util.Log.d("MainActivity", "Notification shown successfully")
                                            } catch (e: Exception) {
                                                android.util.Log.e("MainActivity", "Error in onSuccess callback", e)
                                                e.printStackTrace()
                                                processedCallIds.remove(callId)
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.e("MainActivity", "Failed to get caller info", e)
                                            // Remove from processed set so we can retry
                                            processedCallIds.remove(callId)
                                        }
                                } else {
                                    // If status changed, remove from processed set
                                    processedCallIds.remove(callId)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Error processing document", e)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error in snapshot listener callback", e)
                        e.printStackTrace()
                    }
                }
            
            android.util.Log.d("MainActivity", "Firestore listener registered successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up call listener", e)
            e.printStackTrace()
        }
    }
    
    private fun showContactOptions(contact: User) {
        val options = arrayOf("Call", "Message")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(contact.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startCall(contact)
                    1 -> startChat(contact)
                }
            }
            .show()
    }
    
    private fun startCall(contact: User) {
        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra("contactId", contact.id)
            putExtra("contactName", contact.name)
            putExtra("contactPhone", contact.phone)
            putExtra("isIncoming", false)
        }
        startActivity(intent)
    }
    
    private fun startChat(contact: User) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("contactId", contact.id)
            putExtra("contactName", contact.name)
        }
        startActivity(intent)
    }
    
    inner class ContactsAdapter(
        private val onContactClick: (User) -> Unit
    ) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {
        
        private var contacts = listOf<User>()
        
        fun submitList(newList: List<User>) {
            contacts = newList
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val binding = ItemContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ContactViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            holder.bind(contacts[position])
        }
        
        override fun getItemCount() = contacts.size
        
        inner class ContactViewHolder(
            private val binding: ItemContactBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(user: User) {
                binding.tvName.text = user.name
                binding.tvPhone.text = user.phone
                binding.root.setOnClickListener {
                    onContactClick(user)
                }
            }
        }
    }
}

