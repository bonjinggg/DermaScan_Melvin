package com.example.dermascanai

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dermascanai.databinding.ActivityDermaEditInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DermaEditInfo : AppCompatActivity() {
    private lateinit var binding: ActivityDermaEditInfoBinding
    private lateinit var provinceSpinner: Spinner
    private lateinit var citySpinner: Spinner
    private lateinit var barangaySpinner: Spinner

    private val calendar = Calendar.getInstance()
    private lateinit var database: FirebaseDatabase

    private var selectedBitmap: Bitmap? = null
    private var encodeImg: String? = null

    private var existingProfileImage: String? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var selectedVerificationBitmap: Bitmap? = null
    private var encodedVerificationImg: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDermaEditInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        val typeList = listOf("Select Type", "Certificate", "License", "Other")
        val typeAdapter = ArrayAdapter(this, R.layout.spinner_item, typeList)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.typeSpinner.adapter = typeAdapter

        val genderList = listOf("Select Gender", "Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(this, R.layout.spinner_item, genderList)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            updateLabel()
        }

        fetchUserData()

        provinceSpinner = binding.spinnerProvince
        citySpinner = binding.spinnerCity
        barangaySpinner = binding.spinnerBarangay

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.birthday.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }
        citySpinner.visibility = View.GONE
        barangaySpinner.visibility = View.GONE


        fetchProvinces()



        binding.gender.adapter = genderAdapter


        binding.uploadBtn.setOnClickListener {
            showImagePickerDialog()
        }

        binding.saveBtn.setOnClickListener {
            encodeImg = selectedBitmap?.let { encodeImage(it) } ?: existingProfileImage


            saveInfo()
        }
        binding.uploadType.setOnClickListener {
            showCertificatePickerDialog()
        }

        val validationText = "Validation*"
        val spannable = SpannableString(validationText)
        spannable.setSpan(
            ForegroundColorSpan(Color.RED),
            validationText.length - 1,
            validationText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.textValid.text = spannable



    }
    private fun showCertificatePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Certificate Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCertificateCamera()
                1 -> openCertificateGallery()
            }
        }
        builder.show()
    }


    private fun saveInfo() {
        val user = database.getReference("dermaInfo")

        // Initialize the map at the beginning
        val userInfoMap = mutableMapOf<String, Any?>()

        val name = binding.name.text?.toString()
        val birthday = binding.birthday.text?.toString()
        val gender = binding.gender.selectedItem?.toString()
        val contact = binding.contact.text?.toString()
        val quote = binding.quote.text?.toString()
        val bio = binding.bio.text?.toString()
        val profileImage = encodeImg ?: ""
        val selectedProvince = binding.spinnerProvince.selectedItem?.toString()
        val selectedCity = binding.spinnerCity.selectedItem?.toString()
        val selectedBarangay = binding.spinnerBarangay.selectedItem?.toString()
        val selectedType = binding.typeSpinner.selectedItem?.toString()

        // Add normal fields
        userInfoMap["name"] = name
        userInfoMap["birthday"] = birthday
        userInfoMap["contact"] = contact
        userInfoMap["quote"] = quote
        userInfoMap["bio"] = bio
        userInfoMap["profileImage"] = profileImage
        userInfoMap["status"] = "pending"

        // Add encoded verification image if available
        encodedVerificationImg = selectedVerificationBitmap?.let { encodeImage(it) }
        if (!encodedVerificationImg.isNullOrEmpty()) {
            userInfoMap["verificationImg"] = encodedVerificationImg!!
        }

        // Add fields if they are valid
        if (!gender.isNullOrBlank() && gender != "-Select Gender-") {
            userInfoMap["gender"] = gender
        }
        if (!selectedProvince.isNullOrBlank() && selectedProvince != "-Choose a Province-") {
            userInfoMap["province"] = selectedProvince
        }
        if (!selectedCity.isNullOrBlank() && selectedCity != "-Choose a City-") {
            userInfoMap["city"] = selectedCity
        }
        if (!selectedBarangay.isNullOrBlank() && selectedBarangay != "-Choose a Barangay-") {
            userInfoMap["barangay"] = selectedBarangay
        }
        if (!selectedType.isNullOrBlank() && selectedType != "Select Type") {
            userInfoMap["uploadType"] = selectedType
        }

        if (userId != null) {
            if(selectedType != null) {
                user.child(userId).updateChildren(userInfoMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "User info saved!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to save info: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }else{
                Toast.makeText(this, "Please provide a validation before proceeding.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }



    private fun updateLabel() {
        val format = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.birthday.setText(format.format(calendar.time))
    }

    private fun fetchProvinces() {
        RetrofitClient.instance.getProvinces().enqueue(object : Callback<List<LocationModel>> {
            override fun onResponse(call: Call<List<LocationModel>>, response: Response<List<LocationModel>>) {
                if (response.isSuccessful) {
                    val provinceList = response.body() ?: return
                    val provinceNames = mutableListOf("-Choose a Province-")
                    val provinceCodes = mutableListOf("")

                    provinceList.forEach {
                        provinceNames.add(it.name)
                        provinceCodes.add(it.code)
                    }

                    val provinceAdapter = ArrayAdapter(this@DermaEditInfo, R.layout.spinner_item, provinceNames)
                    provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    provinceSpinner.adapter = provinceAdapter

                    provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            if (position == 0) {
                                citySpinner.visibility = View.GONE
                                barangaySpinner.visibility = View.GONE
                                binding.textViewCity.visibility = View.GONE
                                binding.textViewBarangay.visibility = View.GONE
                            } else {
                                val provinceCode = provinceCodes[position]
                                fetchCities(provinceCode)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
                }
            }

            override fun onFailure(call: Call<List<LocationModel>>, t: Throwable) {
                Toast.makeText(this@DermaEditInfo, "Failed to load provinces: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchCities(provinceCode: String) {
        RetrofitClient.instance.getCities(provinceCode).enqueue(object : Callback<List<LocationModel>> {
            override fun onResponse(call: Call<List<LocationModel>>, response: Response<List<LocationModel>>) {
                if (response.isSuccessful) {
                    val cityList = response.body() ?: return
                    val cityNames = mutableListOf("-Choose a City-")
                    val cityCodes = mutableListOf("")

                    cityList.forEach {
                        cityNames.add(it.name)
                        cityCodes.add(it.code)
                    }

                    val cityAdapter = ArrayAdapter(this@DermaEditInfo, R.layout.spinner_item, cityNames)
                    cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    citySpinner.adapter = cityAdapter

                    citySpinner.visibility = View.VISIBLE
                    binding.textViewCity.visibility = View.VISIBLE

                    citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            if (position == 0) {
                                barangaySpinner.visibility = View.GONE
                                binding.textViewBarangay.visibility = View.GONE
                            } else {
                                val cityCode = cityCodes[position]
                                fetchBarangays(cityCode)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
                }
            }

            override fun onFailure(call: Call<List<LocationModel>>, t: Throwable) {
                Toast.makeText(this@DermaEditInfo, "Failed to load cities: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchBarangays(cityCode: String) {
        RetrofitClient.instance.getBarangays(cityCode).enqueue(object : Callback<List<LocationModel>> {
            override fun onResponse(call: Call<List<LocationModel>>, response: Response<List<LocationModel>>) {
                if (response.isSuccessful) {
                    val barangayList = response.body() ?: return
                    val barangayNames = mutableListOf("-Choose a Barangay-")

                    barangayList.forEach {
                        barangayNames.add(it.name)
                    }

                    val barangayAdapter = ArrayAdapter(this@DermaEditInfo, R.layout.spinner_item, barangayNames)
                    barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    barangaySpinner.adapter = barangayAdapter

                    barangaySpinner.visibility = View.VISIBLE
                    binding.textViewBarangay.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<List<LocationModel>>, t: Throwable) {
                Toast.makeText(this@DermaEditInfo, "Failed to load barangays: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_CODE)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, REQUEST_CAMERA)
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_GALLERY)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    val photo = data?.extras?.get("data") as Bitmap
                    selectedBitmap = photo
                    binding.profPic.setImageBitmap(photo)
                }
                REQUEST_GALLERY -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let {
                        selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                        } else {
                            val source = ImageDecoder.createSource(this.contentResolver, it)
                            ImageDecoder.decodeBitmap(source)
                        }
                        binding.profPic.setImageBitmap(selectedBitmap)
                    }
                }
                REQUEST_CERTIFICATE_CAMERA -> {
                    val photo = data?.extras?.get("data") as Bitmap
                    selectedVerificationBitmap= photo
                    Toast.makeText(this, "Certificate Image Selected (Camera)", Toast.LENGTH_SHORT).show()
                }
                REQUEST_CERTIFICATE_GALLERY -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let {
                        selectedVerificationBitmap = if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                        } else {
                            val source = ImageDecoder.createSource(this.contentResolver, it)
                            ImageDecoder.decodeBitmap(source)
                        }
                    }
                    Toast.makeText(this, "Certificate Image Selected (Gallery)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun openCertificateCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_CODE)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, REQUEST_CERTIFICATE_CAMERA)
        }
    }
    private fun fetchUserData() {
        val userRef: DatabaseReference = database.getReference("dermaInfo").child(userId ?: return)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val userInfo = snapshot.getValue(UserInfo::class.java)

                binding.name.setText(userInfo?.name ?: "")
                binding.birthday.setText(userInfo?.birthday ?: "")
                binding.contact.setText(userInfo?.contact ?: "")
                binding.quote.setText(userInfo?.quote ?: "")
                binding.bio.setText(userInfo?.bio ?: "")


                // Set image
                userInfo?.profileImage?.let {
                    if (it.isNotEmpty()) {
                        existingProfileImage = it
                        val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        binding.profPic.setImageBitmap(bitmap)
                    }
                }
            } else {
                Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch user data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCertificateGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_CERTIFICATE_GALLERY)
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
        private const val REQUEST_GALLERY = 102
        private val REQUEST_CERTIFICATE_CAMERA = 1004
        private val REQUEST_CERTIFICATE_GALLERY = 1005
    }
}