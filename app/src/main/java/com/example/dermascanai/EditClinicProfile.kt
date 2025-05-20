package com.example.dermascanai

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityDermaEditInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditClinicProfile : AppCompatActivity() {
    private lateinit var binding: ActivityDermaEditInfoBinding
    private lateinit var database: FirebaseDatabase

    private var selectedLogoImage: Bitmap? = null
    private var selectedBIRImage: Bitmap? = null
    private var selectedPermitImage: Bitmap? = null

    private var existingLogoImage: String? = null
    private var existingBIRImage: String? = null
    private var existingPermitImage: String? = null

    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // RecyclerView adapters
    private lateinit var servicesAdapter: ServicesAdapter
    private lateinit var dermatologistsAdapter: DermatologistsAdapter
    private val servicesList = mutableListOf<String>()
    private val dermatologistsList = mutableListOf<Dermatologist>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDermaEditInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        setupViews()
        setupRecyclerViews()
        fetchClinicData()
        setupClickListeners()
    }

    private fun setupViews() {
        val operatingDaysList = listOf(
            "Monday - Friday",
            "Monday - Saturday",
            "Tuesday - Saturday",
            "Wednesday - Sunday",
            "Monday - Sunday",
            "Custom Schedule"
        )

        val operatingDaysAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, operatingDaysList)
        binding.editOperatingDays.setAdapter(operatingDaysAdapter)
    }

    private fun setupRecyclerViews() {
        // Services RecyclerView
        servicesAdapter = ServicesAdapter(servicesList) { position ->
            removeService(position)
        }
        binding.servicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EditClinicProfile)
            adapter = servicesAdapter
        }

        // Dermatologists RecyclerView
        dermatologistsAdapter = DermatologistsAdapter(dermatologistsList) { position ->
            removeDermatologist(position)
        }
        binding.dermatologistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EditClinicProfile)
            adapter = dermatologistsAdapter
        }

        updateVisibility()
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.saveBtn.setOnClickListener {
            saveClinicProfile()
        }

        binding.changePhotoBtn.setOnClickListener {
            showImagePickerDialog(IMAGE_TYPE_LOGO)
        }

        binding.editOpeningTime.setOnClickListener {
            showTimePicker(true)
        }

        binding.editClosingTime.setOnClickListener {
            showTimePicker(false)
        }

        binding.addServiceBtn.setOnClickListener {
            showAddServiceDialog()
        }

        binding.addDermatologistBtn.setOnClickListener {
            showAddDermatologistDialog()
        }

        binding.uploadBirBtn.setOnClickListener {
            showImagePickerDialog(IMAGE_TYPE_BIR)
        }

        binding.uploadPermitBtn.setOnClickListener {
            showImagePickerDialog(IMAGE_TYPE_PERMIT)
        }
    }

    private fun showTimePicker(isOpeningTime: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            val formattedTime = timeFormat.format(calendar.time)

            if (isOpeningTime) {
                binding.editOpeningTime.setText(formattedTime)
            } else {
                binding.editClosingTime.setText(formattedTime)
            }
        }, hour, minute, false).show()
    }

    private fun showAddServiceDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter service name"

        AlertDialog.Builder(this)
            .setTitle("Add Service")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val serviceName = input.text.toString().trim()
                if (serviceName.isNotEmpty()) {
                    servicesList.add(serviceName)
                    servicesAdapter.notifyItemInserted(servicesList.size - 1)
                    updateVisibility()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showAddDermatologistDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_dermatologist, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dermatologistName)
        val specializationInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dermatologistSpecialization)

        AlertDialog.Builder(this)
            .setTitle("Add Dermatologist")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val specialization = specializationInput.text.toString().trim()
                if (name.isNotEmpty() && specialization.isNotEmpty()) {
                    val dermatologist = Dermatologist(name, specialization)
                    dermatologistsList.add(dermatologist)
                    dermatologistsAdapter.notifyItemInserted(dermatologistsList.size - 1)
                    updateVisibility()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeService(position: Int) {
        servicesList.removeAt(position)
        servicesAdapter.notifyItemRemoved(position)
        updateVisibility()
    }

    private fun removeDermatologist(position: Int) {
        dermatologistsList.removeAt(position)
        dermatologistsAdapter.notifyItemRemoved(position)
        updateVisibility()
    }

    private fun updateVisibility() {
        binding.noServicesText.visibility = if (servicesList.isEmpty()) View.VISIBLE else View.GONE
        binding.noDermatologistsText.visibility = if (dermatologistsList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showImagePickerDialog(imageType: Int) {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera(imageType)
                1 -> openGallery(imageType)
            }
        }
        builder.show()
    }

    private fun openCamera(imageType: Int) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_CODE)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, REQUEST_CAMERA + imageType)
        }
    }

    private fun openGallery(imageType: Int) {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_GALLERY + imageType)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when {
                requestCode in REQUEST_CAMERA..REQUEST_CAMERA + 2 -> {
                    val photo = data?.extras?.get("data") as Bitmap
                    val imageType = requestCode - REQUEST_CAMERA
                    handleImageResult(photo, imageType)
                }
                requestCode in REQUEST_GALLERY..REQUEST_GALLERY + 2 -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let {
                        val bitmap = if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                        } else {
                            val source = ImageDecoder.createSource(this.contentResolver, it)
                            ImageDecoder.decodeBitmap(source)
                        }
                        val imageType = requestCode - REQUEST_GALLERY
                        handleImageResult(bitmap, imageType)
                    }
                }
            }
        }
    }

    private fun handleImageResult(bitmap: Bitmap, imageType: Int) {
        when (imageType) {
            IMAGE_TYPE_LOGO -> {
                selectedLogoImage = bitmap
                binding.clinicLogo.setImageBitmap(bitmap)
                Toast.makeText(this, "Logo updated", Toast.LENGTH_SHORT).show()
            }
            IMAGE_TYPE_BIR -> {
                selectedBIRImage = bitmap
                binding.birDocument.setImageBitmap(bitmap)
                binding.uploadBirBtn.visibility = View.GONE
                Toast.makeText(this, "BIR document updated", Toast.LENGTH_SHORT).show()
            }
            IMAGE_TYPE_PERMIT -> {
                selectedPermitImage = bitmap
                binding.permitDocument.setImageBitmap(bitmap)
                binding.uploadPermitBtn.visibility = View.GONE
                Toast.makeText(this, "Business permit updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchClinicData() {
        val clinicRef: DatabaseReference = database.getReference("clinicInfo").child(userId ?: return)

        clinicRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val clinicInfo = snapshot.getValue(ClinicInfo::class.java)
                clinicInfo?.let { populateFields(it) }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch clinic data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateFields(clinicInfo: ClinicInfo) {
        binding.editClinicName.setText(clinicInfo.name ?: "")
        binding.editTagline.setText(clinicInfo.tagline ?: "")
        binding.acceptingPatientsCheckbox.isChecked = clinicInfo.acceptingPatients ?: false
        binding.editContact.setText(clinicInfo.contact ?: "")
        binding.editEmail.setText(clinicInfo.email ?: "")
        binding.editAddress.setText(clinicInfo.address ?: "")
        binding.editOperatingDays.setText(clinicInfo.operatingDays ?: "")
        binding.editOpeningTime.setText(clinicInfo.openingTime ?: "")
        binding.editClosingTime.setText(clinicInfo.closingTime ?: "")
        binding.editAbout.setText(clinicInfo.about ?: "")

        // Load images
        clinicInfo.logoImage?.let {
            if (it.isNotEmpty()) {
                existingLogoImage = it
                val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.clinicLogo.setImageBitmap(bitmap)
            }
        }

        clinicInfo.birDocument?.let {
            if (it.isNotEmpty()) {
                existingBIRImage = it
                val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.birDocument.setImageBitmap(bitmap)
                binding.uploadBirBtn.visibility = View.GONE
            }
        }

        clinicInfo.permitDocument?.let {
            if (it.isNotEmpty()) {
                existingPermitImage = it
                val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.permitDocument.setImageBitmap(bitmap)
                binding.uploadPermitBtn.visibility = View.GONE
            }
        }

        // Load services
        clinicInfo.services?.let {
            servicesList.clear()
            servicesList.addAll(it)
            servicesAdapter.notifyDataSetChanged()
        }

        // Load dermatologists
        clinicInfo.dermatologists?.let {
            dermatologistsList.clear()
            dermatologistsList.addAll(it)
            dermatologistsAdapter.notifyDataSetChanged()
        }

        updateVisibility()
    }

    private fun saveClinicProfile() {
        val clinicRef = database.getReference("clinicInfo")

        val clinicInfoMap = mutableMapOf<String, Any?>()

        clinicInfoMap["name"] = binding.editClinicName.text?.toString()
        clinicInfoMap["tagline"] = binding.editTagline.text?.toString()
        clinicInfoMap["acceptingPatients"] = binding.acceptingPatientsCheckbox.isChecked

        clinicInfoMap["contact"] = binding.editContact.text?.toString()
        clinicInfoMap["email"] = binding.editEmail.text?.toString()
        clinicInfoMap["address"] = binding.editAddress.text?.toString()

        clinicInfoMap["operatingDays"] = binding.editOperatingDays.text?.toString()
        clinicInfoMap["openingTime"] = binding.editOpeningTime.text?.toString()
        clinicInfoMap["closingTime"] = binding.editClosingTime.text?.toString()

        clinicInfoMap["about"] = binding.editAbout.text?.toString()

        clinicInfoMap["services"] = servicesList
        clinicInfoMap["dermatologists"] = dermatologistsList


        clinicInfoMap["logoImage"] = selectedLogoImage?.let { encodeImage(it) } ?: existingLogoImage
        clinicInfoMap["birDocument"] = selectedBIRImage?.let { encodeImage(it) } ?: existingBIRImage
        clinicInfoMap["permitDocument"] = selectedPermitImage?.let { encodeImage(it) } ?: existingPermitImage

        if (userId != null) {
            clinicRef.child(userId).updateChildren(clinicInfoMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Clinic profile saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save profile: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    companion object {
        private const val PERMISSION_CODE = 100
        private const val REQUEST_CAMERA = 101
        private const val REQUEST_GALLERY = 201

        private const val IMAGE_TYPE_LOGO = 0
        private const val IMAGE_TYPE_BIR = 1
        private const val IMAGE_TYPE_PERMIT = 2
    }
}

