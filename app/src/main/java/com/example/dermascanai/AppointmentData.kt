package com.example.dermascanai

data class Appointment(
    val bookingId: String = "",
    val patientName: String = "",
    val patientEmail: String = "",
    val doctorName: String = "",
    val date: String = "",
    val time: String = "",
    val timestampMillis: Long = 0,
    val status: String = "pending",
    val message: String = "",
    val cancellationTimestamp: Long = 0,
    val cancellationReason: String? = ""
)
