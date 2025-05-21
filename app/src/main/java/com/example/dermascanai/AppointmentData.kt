package com.example.dermascanai

data class Appointment(
    val bookingId: String = "",
    val patientEmail: String = "",
    val doctorName: String = "",  // This is actually clinic name in the database
    val date: String = "",
    val time: String = "",
    val service: String = "",
    val message: String = "",
    val status: String = "pending",
    val timestampMillis: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val cancellationReason: String? = null,
    // New fields for rescheduling
    val rescheduleRequestTimestamp: Long = 0,
    val requestedNewDate: String = "",
    val requestedNewTime: String = ""
)
