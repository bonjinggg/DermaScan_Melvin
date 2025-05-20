package com.example.dermascanai

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityClinicProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ClinicProfile: AppCompatActivity() {
    private lateinit var binding: ActivityClinicProfileBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var servicesAdapter: ServicesViewAdapter
    private lateinit var dermatologistsAdapter: DermatologistsViewAdapter
    private val servicesList = mutableListOf<String>()
    private val dermatologistsList = mutableListOf<Dermatologist>()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClinicProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        setupClickListeners()
        setupRecyclerViews()
        fetchClinicData()
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.editProfileBtn.setOnClickListener {
            val intent = Intent(this, EditClinicProfile::class.java)
            startActivity(intent)
        }


        binding.birDocument.setOnClickListener {
            Toast.makeText(this, "BIR Document", Toast.LENGTH_SHORT).show()
        }

        binding.permitDocument.setOnClickListener {
            Toast.makeText(this, "Business Permit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        servicesAdapter = ServicesViewAdapter(servicesList)
        binding.servicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClinicProfile)
            adapter = servicesAdapter
        }

        dermatologistsAdapter = DermatologistsViewAdapter(dermatologistsList)
        binding.dermatologistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClinicProfile)
            adapter = dermatologistsAdapter
        }
    }

    private fun fetchClinicData() {
        val clinicRef: DatabaseReference = database.getReference("clinicInfo").child(userId ?: return)

        clinicRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val clinicInfo = snapshot.getValue(ClinicInfo::class.java)
                clinicInfo?.let { populateViews(it) }
            } else {
                Toast.makeText(this, "No clinic data found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch clinic data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateViews(clinicInfo: ClinicInfo) {
        // Profile Header
        binding.displayName.text = clinicInfo.name ?: "Clinic Name"
        binding.displayTagline.text = clinicInfo.tagline ?: "Clinic tagline will appear here..."

        // Accepting patients status
        if (clinicInfo.acceptingPatients == true) {
            binding.acceptingPatientsStatus.visibility = View.VISIBLE
            binding.acceptingPatientsStatus.text = "Accepting New Patients"
            binding.acceptingPatientsStatus.setBackgroundResource(R.drawable.badge_green)
        } else {
            binding.acceptingPatientsStatus.visibility = View.VISIBLE
            binding.acceptingPatientsStatus.text = "Not Accepting"
            binding.acceptingPatientsStatus.setBackgroundResource(R.drawable.badge_red)
        }

        // Clinic Information
        binding.displayContact.text = clinicInfo.contact ?: "Not specified"
        binding.displayEmail.text = clinicInfo.email ?: "Not specified"
        binding.displayAddress.text = clinicInfo.address ?: "Not specified"
        binding.displayOperatingDays.text = clinicInfo.operatingDays ?: "Not specified"

        // Operating hours (combine opening and closing time)
        val openingTime = clinicInfo.openingTime
        val closingTime = clinicInfo.closingTime
        binding.displayOperatingHours.text = when {
            openingTime != null && closingTime != null -> "$openingTime - $closingTime"
            else -> "Not specified"
        }

        // About section
        binding.displayAbout.text = clinicInfo.about ?: "Clinic description will appear here..."

        // Load clinic logo
        clinicInfo.logoImage?.let { logoBase64 ->
            if (logoBase64.isNotEmpty()) {
                val decodedBytes = Base64.decode(logoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.clinicLogo.setImageBitmap(bitmap)
            }
        }

        // Load verification documents
        clinicInfo.birDocument?.let { birBase64 ->
            if (birBase64.isNotEmpty()) {
                val decodedBytes = Base64.decode(birBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.birDocument.setImageBitmap(bitmap)
            }
        }

        clinicInfo.permitDocument?.let { permitBase64 ->
            if (permitBase64.isNotEmpty()) {
                val decodedBytes = Base64.decode(permitBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.permitDocument.setImageBitmap(bitmap)
            }
        }

        // Load services
        clinicInfo.services?.let { services ->
            servicesList.clear()
            servicesList.addAll(services)
            servicesAdapter.notifyDataSetChanged()
        }

        // Load dermatologists
        clinicInfo.dermatologists?.let { dermatologists ->
            dermatologistsList.clear()
            dermatologistsList.addAll(dermatologists)
            dermatologistsAdapter.notifyDataSetChanged()
        }

        // Set verification status
        setVerificationStatus(clinicInfo.status)
    }

    private fun setVerificationStatus(status: String?) {
        when (status) {
            "verified" -> {
                binding.verifiedBadge.setImageResource(R.drawable.badge_green)
                binding.displayVerificationType.text = "Verification: Business Permits & Licenses ✓"
            }
            "pending" -> {
                binding.verifiedBadge.setImageResource(R.drawable.badge_yellow) // You'll need this drawable
                binding.displayVerificationType.text = "Verification: Pending Review"
            }
            "rejected" -> {
                binding.verifiedBadge.setImageResource(R.drawable.badge_red) // You'll need this drawable
                binding.displayVerificationType.text = "Verification: Documents Rejected"
            }
            else -> {
                binding.displayVerificationType.text = "Verification: Not Submitted"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from edit screen
        fetchClinicData()
    }
}