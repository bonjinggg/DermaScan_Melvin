package com.example.dermascanai

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dermascanai.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.mindrot.jbcrypt.BCrypt
import java.io.ByteArrayOutputStream

class Register : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference
    private lateinit var userRole: String


    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2
    private val REQUEST_CAMERA_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("userInfo")




        binding.namelayout.hint = Html.fromHtml(getString(R.string.full_name), Html.FROM_HTML_MODE_LEGACY)
        binding.emailLayout.hint = Html.fromHtml(getString(R.string.email), Html.FROM_HTML_MODE_LEGACY)
        binding.adminPass.hint = Html.fromHtml(getString(R.string.password), Html.FROM_HTML_MODE_LEGACY)
        binding.confirmLayout.hint = Html.fromHtml(getString(R.string.confirm_password), Html.FROM_HTML_MODE_LEGACY)

        userRole = intent.getStringExtra("USER_ROLE") ?: "user"

        binding.uploadBtn.setOnClickListener {
            showImagePickerDialog()
        }

        binding.submit.setOnClickListener {
            registerUser()
        }

        binding.backBTN.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val fullName = binding.name.text.toString().trim()

        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.confirm.text.toString().trim()


        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show()
            return
        }

        val base64Image = encodeImageToBase64(selectedBitmap!!)
        val hashedPassword = hashPassword(password)

        val newUser = UserInfo(fullName, email, hashedPassword, userRole, base64Image)

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val newUserId = mAuth.currentUser?.uid
                    if (newUserId != null) {
                        mDatabase.child(newUserId).setValue(newUser)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    FirebaseAuth.getInstance().signOut()
                                    toLogin()
                                } else {
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                }
                            }

                    }
                } else {
                    Toast.makeText(this, "Auth failed: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun toLogin() {
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Profile Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        requestCameraPermission()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri? {
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "TempImage", null)
        return if (path != null) Uri.parse(path) else null
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openCameraIntent()
        }
    }

    private fun openCameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    data?.data?.let {
                        selectedImageUri = it
                        selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                        binding.profPic.setImageURI(it)
                    }
                }

                REQUEST_IMAGE_CAPTURE -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    if (photo != null) {
                        selectedBitmap = photo
                        selectedImageUri = getImageUriFromBitmap(photo)
                        binding.profPic.setImageBitmap(photo)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraIntent()
            } else {
                Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
