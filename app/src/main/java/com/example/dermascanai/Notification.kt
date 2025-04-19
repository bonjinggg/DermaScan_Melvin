package com.example.dermascanai

data class Notification(
    val notificationId: String = "",
    val postId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val type: String = "", // "like" or "comment"
    val message: String = "",
    val timestamp: Long = 0,
    var isRead: Boolean = false
)
