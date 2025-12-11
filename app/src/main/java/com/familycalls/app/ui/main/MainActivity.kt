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
import com.familycalls.app.service.MessageNotificationManager
import com.familycalls.app.ui.chat.ChatActivity
import com.familycalls.app.data.model.Message
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
    private val contactsAdapter = ContactsAdapter(
        onContactClick = { contact ->
            // Card click opens chat
            startChat(contact)
        },
        onVideoCallClick = { contact ->
            // Video icon click starts video call
            startCall(contact)
        }
    )
    private val callsAdapter = CallHistoryAdapter()
    
    private var currentTab: Tab = Tab.CHATS
    private enum class Tab { CHATS, CALLS }
    
    private var currentProfileDialog: androidx.appcompat.app.AlertDialog? = null
    private var currentDialogAvatarView: android.widget.ImageView? = null
    
    // Track processed messages to avoid duplicate notifications
    private val processedMessageIds = mutableSetOf<String>()
    private var messageListener: com.google.firebase.firestore.ListenerRegistration? = null

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
        private const val REQUEST_CODE_AVATAR = 102
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
        
        // Hide the action bar (remove black header)
        supportActionBar?.hide()
        
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
            
            binding.recyclerViewCalls.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewCalls.adapter = callsAdapter
            
            // Setup tab switching
            setupTabs()
            
            android.util.Log.d("MainActivity", "RecyclerView setup complete")
            
            loadContacts()
            loadCallHistory()
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
            
            // Listen for new messages
            try {
                listenForNewMessages()
                android.util.Log.d("MainActivity", "listenForNewMessages() called")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in listenForNewMessages()", e)
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
        popup.menu.add("Profile Edit")
        popup.menu.add("Share App")
        
        popup.setOnMenuItemClickListener { item ->
            when(item.title) {
                "Profile Edit" -> {
                    showProfileEditDialog()
                    true
                }
                "Share App" -> {
                    shareDownloadLink()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    
    private fun showProfileEditDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .create()
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_edit, null)
        dialog.setView(dialogView)
        
        val cvAvatar = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.cvAvatar)
        val ivAvatar = dialogView.findViewById<android.widget.ImageView>(R.id.ivAvatar)
        val btnChangeAvatar = dialogView.findViewById<android.widget.Button>(R.id.btnChangeAvatar)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etName)
        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.etPhone)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<android.widget.Button>(R.id.btnSave)
        
        // Store references for avatar selection
        currentProfileDialog = dialog
        currentDialogAvatarView = ivAvatar
        
        // Load current user data
        FirebaseFirestore.getInstance().collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                    user?.let {
                        etName?.setText(it.name)
                        etPhone?.setText(it.phone)
                        
                        // Load avatar
                        if (it.avatarUrl.isNotEmpty()) {
                            com.bumptech.glide.Glide.with(this)
                                .load(it.avatarUrl)
                                .circleCrop()
                                .into(ivAvatar ?: return@addOnSuccessListener)
                            cvAvatar?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                        } else {
                            ivAvatar?.setImageResource(android.R.drawable.sym_def_app_icon)
                            ivAvatar?.setColorFilter(android.graphics.Color.WHITE)
                            cvAvatar?.setCardBackgroundColor(0xFFE0E0E0.toInt())
                        }
                    }
                }
            }
        
        btnChangeAvatar?.setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            try {
                startActivityForResult(Intent.createChooser(intent, "Select Avatar"), REQUEST_CODE_AVATAR)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error opening image picker", e)
                android.widget.Toast.makeText(this, "Error opening image picker", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        btnCancel?.setOnClickListener {
            currentProfileDialog = null
            currentDialogAvatarView = null
            dialog.dismiss()
        }
        
        btnSave?.setOnClickListener {
            val newName = etName?.text?.toString()?.trim() ?: ""
            val newPhone = etPhone?.text?.toString()?.trim() ?: ""
            
            if (newName.isEmpty()) {
                android.widget.Toast.makeText(this, "Name cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (newPhone.isEmpty()) {
                android.widget.Toast.makeText(this, "Phone cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            btnSave.isEnabled = false
            btnSave.text = "Saving..."
            
            // Check if new avatar was selected (stored in ivAvatar tag)
            val selectedAvatarUri = ivAvatar?.tag as? android.net.Uri
            
            // Upload avatar if selected
            if (selectedAvatarUri != null) {
                uploadAvatarAndUpdateProfile(selectedAvatarUri, newName, newPhone) { success ->
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                    if (success) {
                        currentProfileDialog = null
                        currentDialogAvatarView = null
                        dialog.dismiss()
                        loadContacts() // Refresh contact list
                    }
                }
            } else {
                // Just update name and phone
                updateUserProfile(newName, newPhone, null) { success ->
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                    if (success) {
                        currentProfileDialog = null
                        currentDialogAvatarView = null
                        dialog.dismiss()
                        loadContacts() // Refresh contact list
                    }
                }
            }
        }
        
        dialog.setOnDismissListener {
            currentProfileDialog = null
            currentDialogAvatarView = null
        }
        
        dialog.show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_AVATAR && resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data
            uri?.let { avatarUri ->
                // Update avatar preview in dialog
                currentDialogAvatarView?.let { avatarView ->
                    // Update the parent CardView background
                    val parent = avatarView.parent as? androidx.cardview.widget.CardView
                    parent?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                    
                    com.bumptech.glide.Glide.with(this)
                        .load(avatarUri)
                        .circleCrop()
                        .into(avatarView)
                    
                    // Store the URI in the dialog view tag for later use
                    avatarView.tag = avatarUri
                    android.widget.Toast.makeText(this, "Avatar selected", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun uploadAvatarAndUpdateProfile(
        avatarUri: android.net.Uri,
        name: String,
        phone: String,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val storageRepository = com.familycalls.app.data.repository.StorageRepository()
                val uploadResult = storageRepository.uploadImage(avatarUri, currentUserId)
                
                uploadResult.fold(
                    onSuccess = { avatarUrl ->
                        updateUserProfile(name, phone, avatarUrl, callback)
                    },
                    onFailure = { e ->
                        android.util.Log.e("MainActivity", "Failed to upload avatar", e)
                        android.widget.Toast.makeText(this@MainActivity, "Failed to upload avatar", android.widget.Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error uploading avatar", e)
                android.widget.Toast.makeText(this@MainActivity, "Error uploading avatar", android.widget.Toast.LENGTH_SHORT).show()
                callback(false)
            }
        }
    }
    
    private fun updateUserProfile(
        name: String,
        phone: String,
        avatarUrl: String?,
        callback: (Boolean) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone
        )
        
        if (avatarUrl != null) {
            updates["avatarUrl"] = avatarUrl
        }
        
        FirebaseFirestore.getInstance().collection("users")
            .document(currentUserId)
            .update(updates)
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Profile updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                callback(true)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MainActivity", "Failed to update profile", e)
                android.widget.Toast.makeText(this, "Failed to update profile", android.widget.Toast.LENGTH_SHORT).show()
                callback(false)
            }
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
    
    private fun listenForNewMessages() {
        try {
            if (currentUserId.isEmpty()) {
                android.util.Log.w("MainActivity", "Cannot listen for messages: currentUserId is empty")
                return
            }
            
            if (isFinishing || isActivityDestroyed()) {
                android.util.Log.w("MainActivity", "Activity is finishing, skipping message listener setup")
                return
            }
            
            android.util.Log.d("MainActivity", "Setting up message listener for userId: $currentUserId")
            
            val db = FirebaseFirestore.getInstance()
            val messagesCollection = db.collection("messages")
            
            // Listen for new messages where this user is the receiver
            messageListener = messagesCollection
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("MainActivity", "Error listening for messages", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot == null) {
                        return@addSnapshotListener
                    }
                    
                    try {
                        // Process document changes to detect only new messages
                        for (documentChange in snapshot.documentChanges) {
                            // Only process ADDED documents (new messages)
                            if (documentChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                try {
                                    val document = documentChange.document
                                    val messageId = document.id
                                    
                                    // Skip if we've already processed this message
                                    if (processedMessageIds.contains(messageId)) {
                                        continue
                                    }
                                    
                                    // Skip if this is from cache (initial load of old messages)
                                    // We only want to notify for truly new messages
                                    if (snapshot.metadata.isFromCache) {
                                        // Mark old messages as processed but don't notify
                                        processedMessageIds.add(messageId)
                                        continue
                                    }
                                    
                                    // Parse message
                                    val message = document.toObject(Message::class.java)?.copy(id = messageId)
                                    if (message == null || message.senderId.isEmpty()) {
                                        android.util.Log.w("MainActivity", "Skipping message with invalid data: $messageId")
                                        continue
                                    }
                                    
                                    // Mark as processed
                                    processedMessageIds.add(messageId)
                                    
                                    // Get sender name
                                    db.collection("users").document(message.senderId)
                                        .get()
                                        .addOnSuccessListener { senderDoc ->
                                            val senderName = senderDoc.getString("name") ?: "Unknown"
                                            
                                            // Show notification
                                            val notificationManager = MessageNotificationManager(applicationContext)
                                            
                                            // Determine message text based on type
                                            val messageText = when (message.type) {
                                                com.familycalls.app.data.model.MessageType.TEXT -> message.text
                                                com.familycalls.app.data.model.MessageType.IMAGE -> "📷 Image"
                                                com.familycalls.app.data.model.MessageType.VIDEO -> "🎥 Video"
                                            }
                                            
                                            if (messageText.isNotEmpty()) {
                                                notificationManager.showMessageNotification(
                                                    message.senderId,
                                                    senderName,
                                                    messageText
                                                )
                                                android.util.Log.d("MainActivity", "Message notification shown: $senderName - $messageText")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.e("MainActivity", "Failed to get sender info for message notification", e)
                                        }
                                    
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error processing message document", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error in message listener callback", e)
                    }
                }
            
            android.util.Log.d("MainActivity", "Message listener registered successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up message listener", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove message listener when activity is destroyed
        messageListener?.remove()
        messageListener = null
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
    
    private fun setupTabs() {
        val tvChatsTab = findViewById<android.widget.TextView>(R.id.tvChatsTab)
        val tvCallsTab = findViewById<android.widget.TextView>(R.id.tvCallsTab)
        
        tvChatsTab?.setOnClickListener {
            switchTab(Tab.CHATS)
        }
        tvCallsTab?.setOnClickListener {
            switchTab(Tab.CALLS)
        }
    }
    
    private fun switchTab(tab: Tab) {
        currentTab = tab
        val tvChatsTab = findViewById<android.widget.TextView>(R.id.tvChatsTab)
        val tvCallsTab = findViewById<android.widget.TextView>(R.id.tvCallsTab)
        
        when (tab) {
            Tab.CHATS -> {
                // Active Chat Tab
                tvChatsTab?.setTextColor(ContextCompat.getColor(this, R.color.ios_text_primary))
                tvChatsTab?.background = ContextCompat.getDrawable(this, R.drawable.bg_button_rounded)
                tvChatsTab?.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                tvChatsTab?.elevation = 4f
                
                // Inactive Calls Tab
                tvCallsTab?.setTextColor(ContextCompat.getColor(this, R.color.ios_text_secondary))
                tvCallsTab?.background = null
                tvCallsTab?.elevation = 0f
                
                binding.recyclerViewContacts.visibility = View.VISIBLE
                binding.recyclerViewCalls.visibility = View.GONE
            }
            Tab.CALLS -> {
                // Inactive Chat Tab
                tvChatsTab?.setTextColor(ContextCompat.getColor(this, R.color.ios_text_secondary))
                tvChatsTab?.background = null
                tvChatsTab?.elevation = 0f
                
                // Active Calls Tab
                tvCallsTab?.setTextColor(ContextCompat.getColor(this, R.color.ios_text_primary))
                tvCallsTab?.background = ContextCompat.getDrawable(this, R.drawable.bg_button_rounded)
                tvCallsTab?.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                tvCallsTab?.elevation = 4f
                
                binding.recyclerViewContacts.visibility = View.GONE
                binding.recyclerViewCalls.visibility = View.VISIBLE
            }
        }
    }
    
    private fun loadCallHistory() {
        if (currentUserId.isEmpty()) {
            android.util.Log.w("MainActivity", "Cannot load call history: currentUserId is empty")
            return
        }
        
        try {
            val callsCollection = FirebaseFirestore.getInstance().collection("calls")
            var callsAsCaller = emptyList<CallHistoryItem>()
            var callsAsReceiver = emptyList<CallHistoryItem>()
            
            fun updateCallList() {
                try {
                    val allCalls = (callsAsCaller + callsAsReceiver)
                        .distinctBy { it.id }
                        .sortedByDescending { it.timestamp }
                    callsAdapter.submitList(allCalls, currentUserId)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error updating call list", e)
                }
            }
            
            // Get calls where current user is caller
            callsCollection
                .whereEqualTo("callerId", currentUserId)
                .addSnapshotListener { snapshot1, error1 ->
                    try {
                        if (error1 != null) {
                            android.util.Log.e("MainActivity", "Error loading calls as caller", error1)
                            return@addSnapshotListener
                        }
                        
                        if (snapshot1 != null) {
                            callsAsCaller = snapshot1.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data
                                    if (data != null) {
                                        CallHistoryItem(
                                            id = doc.id,
                                            callerId = (data["callerId"] as? String)?.takeIf { it.isNotEmpty() } ?: "",
                                            receiverId = (data["receiverId"] as? String)?.takeIf { it.isNotEmpty() } ?: "",
                                            status = (data["status"] as? String)?.takeIf { it.isNotEmpty() } ?: "",
                                            timestamp = (data["timestamp"] as? Long) ?: 0L
                                        )
                                    } else null
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error parsing call document ${doc.id}", e)
                                    null
                                }
                            }.filter { it.callerId.isNotEmpty() && it.receiverId.isNotEmpty() }
                            updateCallList()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error in caller listener", e)
                    }
                }
            
            // Get calls where current user is receiver
            callsCollection
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener { snapshot2, error2 ->
                    try {
                        if (error2 != null) {
                            android.util.Log.e("MainActivity", "Error loading calls as receiver", error2)
                            return@addSnapshotListener
                        }
                        
                        if (snapshot2 != null) {
                            callsAsReceiver = snapshot2.documents.mapNotNull { doc ->
                                try {
                                    val data = doc.data
                                    if (data != null) {
                                        CallHistoryItem(
                                            id = doc.id,
                                            callerId = (data["callerId"] as? String)?.takeIf { it.isNotEmpty() } ?: "",
                                            receiverId = (data["receiverId"] as? String)?.takeIf { it.isNotEmpty() } ?: "",
                                            status = (data["status"] as? String)?.takeIf { it.isNotEmpty() } ?: "",
                                            timestamp = (data["timestamp"] as? Long) ?: 0L
                                        )
                                    } else null
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error parsing call document ${doc.id}", e)
                                    null
                                }
                            }.filter { it.callerId.isNotEmpty() && it.receiverId.isNotEmpty() }
                            updateCallList()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Error in receiver listener", e)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in loadCallHistory()", e)
            e.printStackTrace()
        }
    }
    
    data class CallHistoryItem(
        val id: String,
        val callerId: String,
        val receiverId: String,
        val status: String,
        val timestamp: Long
    )
    
    inner class CallHistoryAdapter : RecyclerView.Adapter<CallHistoryAdapter.CallViewHolder>() {
        
        private var calls = listOf<CallHistoryItem>()
        private var currentUserId = ""
        
        fun submitList(newList: List<CallHistoryItem>, userId: String) {
            calls = newList
            currentUserId = userId
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_call_history, parent, false)
            return CallViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
            holder.bind(calls[position])
        }
        
        override fun getItemCount() = calls.size
        
        inner class CallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivAvatar = itemView.findViewById<android.widget.ImageView>(R.id.ivAvatar)
            private val ivCallIcon = itemView.findViewById<android.widget.ImageView>(R.id.ivCallIcon)
            private val tvCallerName = itemView.findViewById<android.widget.TextView>(R.id.tvCallerName)
            private val tvCallTime = itemView.findViewById<android.widget.TextView>(R.id.tvCallTime)
            private val tvCallDate = itemView.findViewById<android.widget.TextView>(R.id.tvCallDate)
            
            fun bind(call: CallHistoryItem) {
                try {
                    val isIncoming = call.receiverId == currentUserId
                    val otherUserId = if (isIncoming) call.callerId else call.receiverId
                    
                    if (otherUserId.isEmpty()) {
                        android.util.Log.w("CallHistoryAdapter", "Empty otherUserId for call ${call.id}")
                        tvCallerName?.text = "Unknown"
                        return
                    }
                    
                    // Set default values first
                    tvCallerName?.text = "Loading..."
                    tvCallTime?.text = ""
                    tvCallDate?.text = ""
                    
                    // Load user info and avatar
                    FirebaseFirestore.getInstance().collection("users")
                        .document(otherUserId)
                        .get()
                        .addOnSuccessListener { doc ->
                            try {
                                if (doc.exists() && doc.data != null) {
                                    val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                                    user?.let {
                                        tvCallerName?.text = it.name
                                        
                                        // Load avatar
                                        if (it.avatarUrl.isNotEmpty()) {
                                            ivAvatar?.let { avatarView ->
                                                com.bumptech.glide.Glide.with(itemView.context)
                                                    .load(it.avatarUrl)
                                                    .circleCrop()
                                                    .placeholder(android.R.drawable.sym_def_app_icon)
                                                    .into(avatarView)
                                            }
                                        } else {
                                            ivAvatar?.setImageResource(android.R.drawable.sym_def_app_icon)
                                        }
                                    } ?: run {
                                        tvCallerName?.text = "Unknown"
                                    }
                                } else {
                                    tvCallerName?.text = "Unknown"
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CallHistoryAdapter", "Error loading user", e)
                                tvCallerName?.text = "Unknown"
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CallHistoryAdapter", "Failed to load user $otherUserId", e)
                            tvCallerName?.text = "Unknown"
                        }
                    
                    // Set call icon based on status
                    ivCallIcon?.let { icon ->
                        when {
                            call.status == "rejected" -> {
                                icon.setImageResource(android.R.drawable.ic_menu_call)
                                icon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                            }
                            call.status == "accepted" -> {
                                icon.setImageResource(android.R.drawable.ic_menu_call)
                                icon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                            }
                            call.status == "ringing" && isIncoming -> {
                                // Missed call (was ringing but never accepted)
                                icon.setImageResource(android.R.drawable.ic_menu_info_details)
                                icon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark))
                            }
                            else -> {
                                icon.setImageResource(android.R.drawable.ic_menu_call)
                                icon.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark))
                            }
                        }
                    }
                    
                    // Format time and date
                    val date = java.util.Date(call.timestamp)
                    val now = java.util.Date()
                    val diff = now.time - call.timestamp
                    
                    tvCallTime?.text = when {
                        call.status == "rejected" -> "Rejected"
                        call.status == "accepted" -> "Answered"
                        call.status == "ringing" && isIncoming -> "Missed"
                        else -> "Outgoing"
                    }
                    
                    tvCallDate?.text = when {
                        diff < 60000 -> "Just now"
                        diff < 3600000 -> "${diff / 60000}m ago"
                        diff < 86400000 -> "${diff / 3600000}h ago"
                        diff < 604800000 -> "${diff / 86400000}d ago"
                        else -> java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(date)
                    }
                    
                    // Click to call back
                    itemView.setOnClickListener {
                        try {
                            val intent = Intent(this@MainActivity, VideoCallActivity::class.java).apply {
                                putExtra("contactId", otherUserId)
                            }
                            this@MainActivity.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("CallHistoryAdapter", "Error starting VideoCallActivity", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallHistoryAdapter", "Error in bind()", e)
                    e.printStackTrace()
                }
            }
        }
    }

    inner class ContactsAdapter(
        private val onContactClick: (User) -> Unit,
        private val onVideoCallClick: (User) -> Unit
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
                
                // Load avatar
                val cvAvatar = binding.root.findViewById<androidx.cardview.widget.CardView>(R.id.cvAvatar)
                val ivAvatar = cvAvatar?.getChildAt(0) as? android.widget.ImageView
                
                if (user.avatarUrl.isNotEmpty()) {
                    com.bumptech.glide.Glide.with(binding.root.context)
                        .load(user.avatarUrl)
                        .circleCrop()
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .into(ivAvatar ?: return)
                    cvAvatar?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                } else {
                    ivAvatar?.setImageResource(android.R.drawable.sym_def_app_icon)
                    ivAvatar?.setColorFilter(android.graphics.Color.WHITE)
                    cvAvatar?.setCardBackgroundColor(0xFFE0E0E0.toInt())
                }
                
                // Card click opens chat
                binding.root.setOnClickListener {
                    onContactClick(user)
                }
                
                // Video icon click starts video call
                binding.ivVideoCall.setOnClickListener {
                    onVideoCallClick(user)
                }
            }
        }
    }
}