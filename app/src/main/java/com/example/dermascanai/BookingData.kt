package com.example.dermascanai

data class BookingData(
    val bookingId: String = "",
    val patientEmail: String = "",
    val doctorEmail: String = "",
    val doctorName: String = "",
    val date: String = "",
    val time: String = "",
    val message: String = "",
    var status: String = "pending", // pending, confirmed, declined, completed
    val timestampMillis: Long = 0L,
    val createdAt: Long = 0L,
    var declineReason: String? = null,
    val timestamp: Long = 0L,
    var cancellationReason: String? = null,
    val completed: Boolean = false,
    val cancelled: Boolean = false
)