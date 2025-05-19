package com.example.dermascanai

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dermascanai.databinding.ActivityDermaDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DermaDetails : AppCompatActivity() {

    private lateinit var binding: ActivityDermaDetailsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var dermaRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDermaDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
        mAuth = FirebaseAuth.getInstance()

        val userEmail = intent.getStringExtra("userEmail")
        if (userEmail == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.backBTN.setOnClickListener {
            finish()
        }

        fetchUserData(userEmail)

        binding.appointmentBtn.setOnClickListener {
            val intent = Intent(this, Booking::class.java)
            intent.putExtra("doctorEmail", userEmail)
            startActivity(intent)
        }
    }

    private fun fetchUserData(userEmail: String) {
        dermaRef = database.getReference("dermaInfo")
        val query = dermaRef.orderByChild("email").equalTo(userEmail)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        val dermaInfo = childSnapshot.getValue(DermaInfo::class.java)
                        if (dermaInfo != null) {

                            // Full name, bio
                            binding.textView19.text = dermaInfo.name ?: ""
                            binding.bio.text = dermaInfo.bio ?: ""

                            // Clinic full address (concatenated with clinicAddress, barangay, city, province)
                            val fullAddress = listOfNotNull(
                                dermaInfo.clinicAddress,
                                dermaInfo.barangay,
                                dermaInfo.city,
                                dermaInfo.province
                            ).joinToString(", ")
                            binding.textView28.text = if (fullAddress.isNotBlank()) fullAddress else "Not available"

                            // Contact info
                            binding.phone.text = dermaInfo.contact ?: "Not available"
                            binding.gmail.text = dermaInfo.email ?: "Not available"

                            // Schedule
                            val clinicDays = if (!dermaInfo.clinicOpenDay.isNullOrBlank() && !dermaInfo.clinicCloseDay.isNullOrBlank())
                                "${dermaInfo.clinicOpenDay} to ${dermaInfo.clinicCloseDay}" else "Not specified"
                            val clinicTimes = if (!dermaInfo.clinicOpenTime.isNullOrBlank() && !dermaInfo.clinicCloseTime.isNullOrBlank())
                                "${dermaInfo.clinicOpenTime} to ${dermaInfo.clinicCloseTime}" else "Not specified"

                            binding.clinicDaysText.text = clinicDays
                            binding.clinicTimeText.text = clinicTimes

                            // Show/hide based on verification
                            val isVerified = dermaInfo.status.equals("verified", ignoreCase = true)
                            val visibleViews = listOf(
                                binding.textView20, binding.phone, binding.gmail,
                                binding.imageView4, binding.rateMe, binding.appointmentBtn, binding.textView28
                            )
                            val goneViews = listOf(binding.text)

                            visibleViews.forEach { it.visibility = if (isVerified) View.VISIBLE else View.GONE }
                            goneViews.forEach { it.visibility = if (isVerified) View.GONE else View.VISIBLE }

                            // Profile image
                            dermaInfo.profileImage?.takeIf { it.isNotBlank() }?.let {
                                try {
                                    val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    binding.profile.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@DermaDetails, "No matching derma found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DermaDetails, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
