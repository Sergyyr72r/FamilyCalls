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
                NotificationManager.IMPORTANCE_HIGH // High importance for pop-up notifications
            ).apply {
                description = "New message notifications"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
            }
            notificationManager.createNotificationChannel(channel)
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
        
        // Truncate message text for preview (max 100 chars)
        val previewText = if (messageText.length > 100) {
            messageText.substring(0, 97) + "..."
        } else {
            messageText
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(senderName)
            .setContentText(previewText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(messageText)
                .setBigContentTitle(senderName))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for pop-up
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()
        
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

