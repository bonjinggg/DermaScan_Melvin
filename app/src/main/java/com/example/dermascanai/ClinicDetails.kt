package com.example.dermascanai

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityDermaDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ClinicDetails : AppCompatActivity() {

    private lateinit var binding: ActivityDermaDetailsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var clinicRef: DatabaseReference
    private lateinit var dermatologistsAdapter: DermatologistsViewAdapter
    private val dermatologistsList = mutableListOf<Dermatologist>()
    private lateinit var servicesAdapter: ServicesViewAdapter
    private val servicesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDermaDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        dermatologistsAdapter = DermatologistsViewAdapter(dermatologistsList)
        binding.dermatologistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClinicDetails)
            adapter = dermatologistsAdapter
        }


        val clinicEmail = intent.getStringExtra("userEmail")
        if (clinicEmail == null) {
            Toast.makeText(this, "Clinic email not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        fetchClinicData(clinicEmail)

        binding.backBtn.setOnClickListener {
            finish()
        }


        binding.appointmentBtn.setOnClickListener {
            val intent = Intent(this, Booking::class.java)
            intent.putExtra("clinicEmail", clinicEmail)
            startActivity(intent)
        }

        binding.rateMe.setOnClickListener {
            // Handle rate me button click
            // You can implement rating functionality here
        }
    }

    private fun setupRecyclerView() {
        servicesAdapter = ServicesViewAdapter(servicesList)
        binding.servicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClinicDetails)
            adapter = servicesAdapter
        }
    }

    private fun fetchClinicData(clinicEmail: String) {
        clinicRef = database.getReference("clinicInfo")
        val query = clinicRef.orderByChild("email").equalTo(clinicEmail)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        val clinicInfo = childSnapshot.getValue(ClinicInfo::class.java)
                        if (clinicInfo != null) {
                            populateViews(clinicInfo)
                        }
                    }
                } else {
                    Toast.makeText(this@ClinicDetails, "No matching clinic found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ClinicDetails, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateViews(clinicInfo: ClinicInfo) {

        binding.displayName.text = clinicInfo.name ?: "Clinic Name"

        binding.ratingText.text = clinicInfo.rating?.toString() ?: "4.5"

        // Schedule Information
        binding.clinicDaysText.text = clinicInfo.operatingDays ?: "Not specified"

        val openingTime = clinicInfo.openingTime
        val closingTime = clinicInfo.closingTime
        binding.clinicTimeText.text = when {
            openingTime != null && closingTime != null -> "$openingTime to $closingTime"
            else -> "Hours not specified"
        }

        // Contact Information
        binding.phone.text = clinicInfo.contact ?: "Contact not specified"
        binding.clinicEmail.text = clinicInfo.email ?: "Email not specified"
        binding.displayAddress.text = clinicInfo.address ?: "Address not specified"

        // About Section
        binding.bio.text = clinicInfo.about ?: "Clinic bio and description will appear here..."

        // Load clinic profile image
        clinicInfo.logoImage?.let { logoBase64 ->
            if (logoBase64.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(logoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    binding.profile.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Load services
        clinicInfo.services?.let { services ->
            if (services.isNotEmpty()) {
                binding.servicesCard.visibility = View.VISIBLE
                servicesList.clear()
                servicesList.addAll(services)
                servicesAdapter.notifyDataSetChanged()
            } else {
                binding.servicesCard.visibility = View.GONE
            }
        } ?: run {
            binding.servicesCard.visibility = View.GONE
        }

        // Load dermatologists
        clinicInfo.dermatologists?.let { dermatologists ->
            if (dermatologists.isNotEmpty()) {
                binding.dermatologistsCard.visibility = View.VISIBLE
                dermatologistsList.clear()
                dermatologistsList.addAll(dermatologists)
                dermatologistsAdapter.notifyDataSetChanged()
            } else {
                binding.dermatologistsCard.visibility = View.GONE
            }
        } ?: run {
            binding.dermatologistsCard.visibility = View.GONE
        }

        when (clinicInfo.status) {
            "verified" -> {
                binding.appointmentBtn.visibility = View.VISIBLE
                binding.appointmentBtn.isEnabled = true
            }
            else -> {
                binding.appointmentBtn.visibility = View.VISIBLE
                binding.appointmentBtn.isEnabled = false
                binding.appointmentBtn.text = "Clinic Not Verified"
            }
        }
    }
}