package com.example.dermascanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Dermatologist(
    val name: String? = null,
    val specialization: String? = null,
    var email: String? = null,
    var phone: String? = null,
    var logoImage: String? = null
) : Parcelable
