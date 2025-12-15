package com.familycalls.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.familycalls.app.R
import com.familycalls.app.ui.chat.ChatActivity

class MessageNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "family_calls_messages_channel"
        private const val NOTIFICATION_ID_PREFIX = 2000 // Different from call notifications
    }
    
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH // High importance for heads-up pop-up notifications
            ).apply {
                description = "New message notifications"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                // Enable sound for message notifications
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                // Allow full visibility on lock screen - CRITICAL for showing message text
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // Allow bubbles (if supported)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
                // Set bypass DND for important messages
                setBypassDnd(false) // Don't bypass, but can be enabled if needed
            }
            
            // Delete and recreate channel to ensure settings take effect
            // (Channels can't be modified after creation, only deleted and recreated)
            try {
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
                android.util.Log.d("MessageNotificationManager", "Deleted old channel to recreate with correct settings")
            } catch (e: Exception) {
                android.util.Log.d("MessageNotificationManager", "Channel doesn't exist yet or can't be deleted", e)
            }
            
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("MessageNotificationManager", "Notification channel created with VISIBILITY_PUBLIC")
        }
    }
    
    fun showMessageNotification(
        senderId: String,
        senderName: String,
        messageText: String
    ) {
        android.util.Log.d("MessageNotificationManager", "Showing notification: $senderName - $messageText")
        
        // Create intent to open chat with this sender
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("contactId", senderId)
            putExtra("contactName", senderName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode(), // Use senderId hash for unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // For short messages, use as-is. For longer messages, show preview in contentText
        // and full text in BigTextStyle
        val contentText = if (messageText.length > 200) {
            messageText.substring(0, 197) + "..."
        } else {
            messageText
        }
        
        android.util.Log.d("MessageNotificationManager", "Creating notification - Title: $senderName, Text: '$contentText'")
        android.util.Log.d("MessageNotificationManager", "Full message text: '$messageText'")
        
        // Ensure we have actual message text
        val displayText = if (contentText.isNotEmpty() && contentText != "New message") {
            contentText
        } else if (messageText.isNotEmpty() && messageText != "New message") {
            // Fallback to full message if contentText is empty
            if (messageText.length > 200) messageText.substring(0, 197) + "..." else messageText
        } else {
            "New message" // Last resort
        }
        
        android.util.Log.d("MessageNotificationManager", "Display text will be: '$displayText'")
        
        // Build notification with WhatsApp/Telegram-style heads-up display
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(senderName) // Sender name as title
            .setContentText(displayText) // Message preview text - THIS SHOULD SHOW THE ACTUAL MESSAGE
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(if (messageText.isNotEmpty()) messageText else displayText) // Full message text
                    .setBigContentTitle(senderName)
                    .setSummaryText("") // Remove summary text for cleaner look
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX) // MAX priority for heads-up pop-up (like WhatsApp/Telegram)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis()) // Show timestamp
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show full content on lock screen - NOT PRIVATE
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setOnlyAlertOnce(false) // Alert for each new message
            .setGroupSummary(false) // Don't group messages
            .build()
        
        android.util.Log.d("MessageNotificationManager", "Notification built successfully")
        
        // Use senderId hash to create unique notification ID per sender
        // This way, each sender gets their own notification that updates
        val notificationId = NOTIFICATION_ID_PREFIX + (senderId.hashCode() and 0x7FFFFFFF)
        notificationManager.notify(notificationId, notification)
        
        android.util.Log.d("MessageNotificationManager", "Notification posted with ID: $notificationId")
    }
    
    fun cancelNotification(senderId: String) {
        val notificationId = NOTIFICATION_ID_PREFIX + (senderId.hashCode() and 0x7FFFFFFF)
        notificationManager.cancel(notificationId)
    }
}



