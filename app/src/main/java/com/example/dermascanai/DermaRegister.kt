package com.example.dermascanai

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dermascanai.databinding.ActivityDermaRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.mindrot.jbcrypt.BCrypt
import java.io.ByteArrayOutputStream

class DermaRegister : AppCompatActivity() {
    private lateinit var binding: ActivityDermaRegisterBinding

    private lateinit var mAuth: FirebaseAuth
    private lateinit var dDatabase: DatabaseReference

    // Profile image
    private var selectedProfileBitmap: Bitmap? = null

    // Document images
    private var birBitmap: Bitmap? = null
    private var businessPermitBitmap: Bitmap? = null
    private var validIdBitmap: Bitmap? = null

    private val REQUEST_PROFILE_IMAGE = 1
    private val REQUEST_BIR = 10
    private val REQUEST_BUSINESS_PERMIT = 11
    private val REQUEST_VALID_ID = 12

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val times = listOf(
        "6:00 AM", "7:00 AM", "8:00 AM", "9:00 AM",
        "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM",
        "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDermaRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        dDatabase = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("clinicInfo")

        setupClinicOpeningSpinners()
        setupClinicClosingSpinners()
        setupSpinnerListeners()

        binding.uploadBtn.setOnClickListener { showImagePickerDialog(REQUEST_PROFILE_IMAGE) }
        binding.uploadBirBtn.setOnClickListener { showImagePickerDialog(REQUEST_BIR) }
        binding.uploadBusinessPermitBtn.setOnClickListener { showImagePickerDialog(REQUEST_BUSINESS_PERMIT) }
        binding.uploadValidIdBtn.setOnClickListener { showImagePickerDialog(REQUEST_VALID_ID) }

        binding.submit.setOnClickListener {
            if (validateInputs()) {
                submitRegistration()
            }
        }

        binding.backBTN.setOnClickListener { finish() }
    }

    private fun setupClinicOpeningSpinners() {
        val dayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClinicOpenDay.adapter = dayAdapter

        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, times)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClinicOpenTime.adapter = timeAdapter
    }

    private fun setupClinicClosingSpinners() {
        // Initially populate Close Day spinner with all days
        val closeDayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        closeDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClinicCloseDay.adapter = closeDayAdapter

        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, times)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClinicCloseTime.adapter = timeAdapter
    }

    private fun setupSpinnerListeners() {
        binding.spinnerClinicOpenDay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOpenDay = days[position]
                // Update Close Day spinner options to exclude selected Open Day
                val closeDayOptions = days.filter { it != selectedOpenDay }
                val closeDayAdapter = ArrayAdapter(this@DermaRegister, android.R.layout.simple_spinner_item, closeDayOptions)
                closeDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerClinicCloseDay.adapter = closeDayAdapter
                // Reset close day selection to first available
                binding.spinnerClinicCloseDay.setSelection(0)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed here
            }
        }
    }

    private fun showImagePickerDialog(requestCode: Int) {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCameraIntent(requestCode)
                1 -> openGalleryIntent(requestCode)
            }
        }
        builder.show()
    }

    private fun openGalleryIntent(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, requestCode)
    }

    private fun openCameraIntent(requestCode: Int) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_PROFILE_IMAGE -> {
                    data?.data?.let { uri ->
                        selectedProfileBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        binding.profPic.setImageURI(uri)
                    } ?: run {
                        val photo = data?.extras?.get("data") as? Bitmap
                        photo?.let {
                            selectedProfileBitmap = it
                            binding.profPic.setImageBitmap(it)
                        }
                    }
                }
                REQUEST_BIR -> {
                    data?.data?.let { uri ->
                        birBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        binding.birImageView.setImageBitmap(birBitmap)
                    }
                }
                REQUEST_BUSINESS_PERMIT -> {
                    data?.data?.let { uri ->
                        businessPermitBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        binding.businessPermitImageView.setImageBitmap(businessPermitBitmap)
                    }
                }
                REQUEST_VALID_ID -> {
                    data?.data?.let { uri ->
                        validIdBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        binding.validIdImageView.setImageBitmap(validIdBitmap)
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.name.text.toString().trim().isEmpty() ||
            binding.email.text.toString().trim().isEmpty() ||
            binding.password.text.toString().trim().isEmpty() ||
            binding.confirm.text.toString().trim().isEmpty() ||
            binding.clinicName.text.toString().trim().isEmpty() ||
            binding.clinicAddress.text.toString().trim().isEmpty() ||
            binding.clinicPhone.text.toString().trim().isEmpty()
        ) {
            Toast.makeText(this, "Please complete all required fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.password.text.toString() != binding.confirm.text.toString()) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        if (birBitmap == null || businessPermitBitmap == null || validIdBitmap == null) {
            Toast.makeText(this, "Please upload all required documents", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!binding.checkBox.isChecked) {
            Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
    private fun submitRegistration() {
        val profileImageEncoded = selectedProfileBitmap?.let { encodeImageToBase64(it) } ?: ""
        val birEncoded = encodeImageToBase64(birBitmap!!)
        val businessPermitEncoded = encodeImageToBase64(businessPermitBitmap!!)
        val validIdEncoded = encodeImageToBase64(validIdBitmap!!)

        val plainPassword = binding.password.text.toString().trim()
        val hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt())

        val selectedOpenDay = binding.spinnerClinicOpenDay.selectedItem as? String ?: ""
        val selectedCloseDay = binding.spinnerClinicCloseDay.selectedItem as? String ?: ""
        val selectedOpenTime = binding.spinnerClinicOpenTime.selectedItem as? String ?: ""
        val selectedCloseTime = binding.spinnerClinicCloseTime.selectedItem as? String ?: ""

        val clinicInfo = ClinicInfo(
            name = binding.name.text.toString().trim(),
            email = binding.email.text.toString().trim(),
            role = "derma",
            contact = binding.phone.text.toString().trim(),

            clinicName = binding.clinicName.text.toString().trim(),
            clinicAddress = binding.clinicAddress.text.toString().trim(),
            clinicPhone = binding.clinicPhone.text.toString().trim(),

            clinicOpenDay = selectedOpenDay,
            clinicCloseDay = selectedCloseDay,
            clinicOpenTime = selectedOpenTime,
            clinicCloseTime = selectedCloseTime,
            birImage = birEncoded,
            businessPermitImage = businessPermitEncoded,
            validIdImage = validIdEncoded,

            password = hashedPassword,

            // For ClinicProfile
            logoImage = profileImageEncoded,
            birDocument = birEncoded,
            permitDocument = businessPermitEncoded,
            address = binding.clinicAddress.text.toString().trim(),
            openingTime = selectedOpenTime,
            closingTime = selectedCloseTime,
            operatingDays = "$selectedOpenDay to $selectedCloseDay",
            tagline = "Welcome to our clinic!",
            about = "Clinic description goes here...",
            status = "pending",
            acceptingPatients = true,
            services = emptyList(),
            dermatologists = emptyList()
        )

        // Firebase Authentication and Database Save
        mAuth.createUserWithEmailAndPassword(clinicInfo.email ?: "", plainPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid
                    if (uid != null) {
                        FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("clinicInfo")
                            .child(uid)
                            .setValue(clinicInfo)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registration complete!", Toast.LENGTH_LONG).show()
                                // ✅ Navigate to ClinicProfile
                                val intent = Intent(this, Login::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save user data: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
    }
}
