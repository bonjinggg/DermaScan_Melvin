package com.example.dermascanai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.dermascanai.databinding.ActivityMainPageBinding
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import android.media.ExifInterface
import android.util.Base64
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainPage : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding
    private lateinit var firebase: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var interpreter: Interpreter? = null


    private val PERMISSION_REQUEST_CODE = 1001
    private val PICK_IMAGE_REQUEST = 1002
    private val CAMERA_REQUEST = 1003

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebase = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        checkPermissions()

        try {
            interpreter = Interpreter(loadModelFile())
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Model loading failed", Toast.LENGTH_LONG).show()
        }

        binding.scanButton.setOnClickListener {
            showImagePickerDialog()
        }

        binding.backBTN.setOnClickListener {
            finish()
        }

    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd("model_derma.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Take a Photo")
        android.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> getSkinImageFromGallery()
                    1 -> takePhoto()
                }
            }
            .show()
    }

    private fun getSkinImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            imageUri = FileProvider.getUriForFile(this, "com.example.dermascanai.fileprovider", photoFile)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(intent, CAMERA_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val bitmap: Bitmap = when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    imageUri = data?.data
                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }
                CAMERA_REQUEST -> {
                    BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri!!))
                }
                else -> return
            }

            CoroutineScope(Dispatchers.Main).launch {
                showProgress()

                val result = withContext(Dispatchers.IO) {
                    val rotatedBitmap = rotateImageIfNeeded(bitmap, imageUri)
                    val predictionResult = predict(rotatedBitmap)
                    predictionResult
                }



                hideProgress()
                binding.detailBtn.visibility = View.VISIBLE
                binding.skinImageView.setImageBitmap(bitmap)
                binding.resultTextView.text = "You might have $result"
                binding.remedyTextView.text = getRemedy(result)

                binding.detailBtn.setOnClickListener {
                    val intent = Intent(this@MainPage, DiseaseDetails::class.java)
                    intent.putExtra("condition", result)
                    intent.putExtra("image", imageUri)
                    startActivity(intent)
                }
                binding.saveScanButton.visibility = View.VISIBLE

                binding.saveScanButton.setOnClickListener {

                    val condition = result
                    val remedy = getRemedy(result)

                    saveScanResultToFirebase(condition, remedy, bitmap)
                }
            }
        }
    }

    private fun rotateImageIfNeeded(bitmap: Bitmap, uri: Uri?): Bitmap {
        val inputStream = contentResolver.openInputStream(uri!!)
        val exif = ExifInterface(inputStream!!)
        val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun predict(bitmap: Bitmap): String {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val input = preprocessImage(resizedBitmap)

        val output = Array(1) { FloatArray(24) }
        interpreter?.run(input, output)

        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        return if (maxIndex != -1) getConditionLabel(maxIndex) else "Unknown"
    }

    private fun preprocessImage(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val result = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val pixel = bitmap.getPixel(i, j)
                result[0][i][j][0] = Color.red(pixel) / 255.0f
                result[0][i][j][1] = Color.green(pixel) / 255.0f
                result[0][i][j][2] = Color.blue(pixel) / 255.0f
            }
        }
        return result
    }

    private fun getConditionLabel(index: Int): String {
//        val conditionLabels = listOf(
//            "Acne", "Actinic Keratosis", "Atopic Dermatitis", "Basal Cell Carcinoma",
//            "Benign Keratosis", "Cellulitis", "Dermatofibroma", "Eczema", "Hemangioma",
//            "Herpes", "Impetigo", "Lichen Planus", "Melanoma", "Molluscum Contagiosum",
//            "Nevus (Mole)", "Psoriasis", "Rosacea", "Scabies", "Seborrheic Keratosis",
//            "Shingles (Herpes Zoster)", "Tinea (Ringworm)", "Urticaria (Hives)", "Vitiligo"
//
//        )
        val conditionLabels = listOf(
            "Acne or Rosacea",
            "Actinic Keratosis, Basal Cell Carcinoma, or other Malignant Lesions",
            "Atopic Dermatitis",
            "Bullous Disease",
            "Cellulitis, Impetigo, or other Bacterial Infections",
            "Eczema",
            "Exanthems or Drug Eruptions",
            "Hair Loss, Alopecia, or other Hair Diseases",
            "Herpes, HPV, or other STDs",
            "Light Diseases or Pigmentation Disorders",
            "Lupus or other Connective Tissue Diseases",
            "Melanoma, Nevi, or Moles",
            "Nail Fungus or other Nail Diseases",
            "Normal",
            "Poison Ivy or other Contact Dermatitis",
            "Psoriasis, Lichen Planus, or related Diseases",
            "Scabies, Lyme Disease, or other Infestations",
            "Seborrheic Keratoses or other Benign Tumors",
            "Systemic Disease",
            "Tinea, Ringworm, Candidiasis, or other Fungal Infections",
            "Urticaria (Hives)",
            "Vascular Tumors",
            "Vasculitis",
            "Warts, Molluscum, or other Viral Infections"
        )

        return conditionLabels.getOrElse(index) { "Unknown" }
    }

    private fun getRemedy(condition: String): String {
        return when (condition) {
            "Acne or Rosacea" -> "Cleanse gently, avoid triggers like spicy food and harsh products; use prescribed topical treatments."
            "Actinic Keratosis, Basal Cell Carcinoma, or other Malignant Lesions" -> "Consult a dermatologist immediately for evaluation and possible surgical or topical treatment."
            "Atopic Dermatitis" -> "Moisturize regularly, avoid allergens, and use prescribed anti-inflammatory creams."
            "Bullous Disease" -> "Seek medical attention; treatment may include corticosteroids or immunosuppressants."
            "Cellulitis, Impetigo, or other Bacterial Infections" -> "Use prescribed antibiotics and keep the area clean; seek medical care promptly."
            "Eczema" -> "Keep skin moisturized, avoid irritants, and use topical steroids if prescribed."
            "Exanthems or Drug Eruptions" -> "Discontinue suspected medications and consult a doctor immediately."
            "Hair Loss, Alopecia, or other Hair Diseases" -> "Consult a dermatologist for diagnosis; treatments may include minoxidil or corticosteroids."
            "Herpes, HPV, or other STDs" -> "Avoid contact during outbreaks; antiviral or topical treatments may be required."
            "Light Diseases or Pigmentation Disorders" -> "Use broad-spectrum sunscreen and follow medical guidance for pigmentation treatments."
            "Lupus or other Connective Tissue Diseases" -> "Seek specialist care; treatment may include immunosuppressive medications."
            "Melanoma, Nevi, or Moles" -> "Monitor for asymmetry or changes; consult a dermatologist for any suspicious spots."
            "Nail Fungus or other Nail Diseases" -> "Use antifungal treatments and keep nails clean and dry; consult a doctor if persistent."
            "Normal" -> "No action needed. Maintain healthy skincare practices."
            "Poison Ivy or other Contact Dermatitis" -> "Avoid contact with allergens, apply calamine lotion or topical steroids for relief."
            "Psoriasis, Lichen Planus, or related Diseases" -> "Moisturize often and follow prescribed treatments like topical corticosteroids or phototherapy."
            "Scabies, Lyme Disease, or other Infestations" -> "Use medicated creams like permethrin; wash clothing and linens thoroughly."
            "Seborrheic Keratoses or other Benign Tumors" -> "Generally harmless; consult a dermatologist if removal is desired."
            "Systemic Disease" -> "Requires thorough evaluation by a healthcare professional; follow tailored medical treatment."
            "Tinea, Ringworm, Candidiasis, or other Fungal Infections" -> "Apply antifungal treatments and keep affected areas dry and clean."
            "Urticaria (Hives)" -> "Take antihistamines and identify potential triggers; seek medical help if persistent."
            "Vascular Tumors" -> "Often monitored over time; some may require medical or surgical treatment."
            "Vasculitis" -> "Seek immediate medical care; treatment depends on severity and underlying cause."
            "Warts, Molluscum, or other Viral Infections" -> "Avoid touching or scratching lesions; some cases resolve on their own, others need treatment."
            else -> "No specific remedy found. Consult a dermatologist for diagnosis and treatment."
        }

    }

//    private fun convertToHSV(bitmap: Bitmap, hsvBitmap: Bitmap) {
//        for (x in 0 until bitmap.width) {
//            for (y in 0 until bitmap.height) {
//                val pixel = bitmap.getPixel(x, y)
//                val hsv = FloatArray(3)
//                Color.colorToHSV(pixel, hsv)
//                hsvBitmap.setPixel(x, y, Color.HSVToColor(hsv))
//            }
//        }
//    }
//
//    private fun applySkinColorMask(hsvBitmap: Bitmap): Bitmap {
//        val width = hsvBitmap.width
//        val height = hsvBitmap.height
//        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//
//        for (x in 0 until width) {
//            for (y in 0 until height) {
//                val pixel = hsvBitmap.getPixel(x, y)
//                val hsv = FloatArray(3)
//                Color.colorToHSV(pixel, hsv)
//
//                val h = hsv[0]
//                val s = hsv[1]
//                val v = hsv[2]
//
//                if (h in 0f..50f && s >= 0.23f && s <= 0.68f && v >= 0.35f && v <= 1f) {
//                    resultBitmap.setPixel(x, y, Color.WHITE)
//                } else {
//                    resultBitmap.setPixel(x, y, Color.BLACK)
//                }
//            }
//        }
//
//        return resultBitmap
//    }


    private fun saveScanResultToFirebase(condition: String, remedy: String, bitmap: Bitmap) {
        val databaseReference = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = encodeImageToBase64(bitmap)
        val timestamp = SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val scanId = SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.getDefault()).format(Date())

        val scanResult = ScanResult(condition, remedy, imageBase64, timestamp)

        databaseReference.child("scanResults").child(userId).child(scanId).setValue(scanResult)
            .addOnSuccessListener {
                Toast.makeText(this, "Scan result saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, Base64.DEFAULT)
    }



    private fun showProgress() {
        binding.loadingProgressBar.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.loadingProgressBar.visibility = View.GONE
    }
}
