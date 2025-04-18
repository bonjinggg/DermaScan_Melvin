package com.example.dermascanai

data class User(
    var fullName: String? = null,
    var email: String? = null,
    var password: String? = null,
    var role: String = "",
    var imageUrl: String? = null
)
