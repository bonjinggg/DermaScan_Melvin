package com.example.dermascanai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DermaInfo(
    var name: String? = null,
    var email: String? = null,
    var password: String = "",
    var role: String = "derma",
    var status: String = "not verified",
    var contact: String? = null,
    var birthday: String? = null,
    var gender: String? = null,
    var province: String? = null,
    var city: String? = null,
    var barangay: String? = null,
    var quote: String? = null,
    var bio: String? = null,
    var verificationImg: String? = null,
    var rating: String? = null,
    var feedback: String? = null,
    var street: String? = null,
    var postalCode: String? = null,

    // Clinic Information combined here
    var clinicName: String? = null,
    var clinicAddress: String? = null,
    var clinicPhone: String? = null,

    // New fields for clinic opening schedule
    var clinicOpenDay: String? = null,
    var clinicOpenTime: String? = null,
    var clinicCloseDay: String? = null,
    var clinicCloseTime: String? = null,

    var profileImage: String? = null,

    // Additional uploaded documents
    var birImage: String? = null,
    var businessPermitImage: String? = null,
    var validIdImage: String? = null
) : Parcelable
