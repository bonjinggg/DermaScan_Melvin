package com.example.dermascanai

data class ScanResult(
    val condition: String = "",
    val remedy: String = "",
    val imageBase64: String = "",
    val timestamp: String = ""
)
