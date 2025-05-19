package com.example.dermascanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class UserInfo(
    var name: String? = null,
    var email: String? = null,
    var password: String = "",
    var role: String = "",
    var profileImage: String? = null,
    var birthday: String? = null,
    var gender: String? = null,
    var contact: String? = null,
    var province: String? = null,
    var city: String? = null,
    var barangay: String? = null,
    var quote: String? = null,
    var bio: String? = null
)


