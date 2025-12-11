package com.familycalls.app.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val deviceId: String = "",
    val fcmToken: String = "", // FCM token for push notifications
    val avatarUrl: String = "", // Avatar picture URL
    val registeredAt: Long = System.currentTimeMillis()
)

