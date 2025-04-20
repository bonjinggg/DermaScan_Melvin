package com.example.dermascanai


import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.example.dermascanai.databinding.ActivityMainPageBinding
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.core.content.FileProvider

class MainPage : AppCompatActivity() {
    private lateinit var binding: ActivityMainPageBinding
    private var interpreter: Interpreter? = null

    private val PERMISSION_REQUEST_CODE = 1001
    private val PICK_IMAGE_REQUEST = 1002
    private val CAMERA_REQUEST = 1003

    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        checkPermissions()


        // Load the model
        try {
            interpreter = Interpreter(loadModelFile())
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Model loading failed", Toast.LENGTH_LONG).show()
        }

        // Button to scan/select image
        binding.scanButton.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd("derma_model.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun showImagePickerDialog() {
        // Show options to pick image or take a photo
        val options = arrayOf("Choose from Gallery", "Take a Photo")
        val dialog = android.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> getSkinImageFromGallery() // Gallery option
                    1 -> takePhoto() // Camera option
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
            // Create a file to save the photo
            val photoFile = createImageFile()

            // Get a content:// URI from FileProvider
            imageUri = FileProvider.getUriForFile(this, "com.example.dermascanai.fileprovider", photoFile)

            // Grant permissions to the camera app to write to this URI
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

            // Start the camera activity
            startActivityForResult(intent, CAMERA_REQUEST)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val bitmap: Bitmap
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    imageUri = data?.data
                    bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }
                CAMERA_REQUEST -> {
                    bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri!!))
                }
                else -> return
            }

            // Rotate the image if needed
            val rotatedBitmap = rotateImageIfNeeded(bitmap, imageUri)
            binding.skinImageView.setImageBitmap(rotatedBitmap)

            val result = predict(rotatedBitmap)
            binding.resultTextView.text = "$result"

            val remedy = getRemedy(result)
            binding.remedyTextView.text = "$remedy"
        }
    }

    private fun rotateImageIfNeeded(bitmap: Bitmap, imageUri: Uri?): Bitmap {
        val inputStream = contentResolver.openInputStream(imageUri!!)
        val exif = ExifInterface(inputStream!!)
        val rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        var rotatedBitmap = bitmap
        when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateBitmap(bitmap, 270f)
        }
        return rotatedBitmap
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun createImageFile(): java.io.File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
        val storageDir: java.io.File = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)!!
        return java.io.File.createTempFile(
            "JPEG_${timeStamp}_",  /* prefix */
            ".jpg",                 /* suffix */
            storageDir              /* directory */
        )
    }

    private fun predict(bitmap: Bitmap): String {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val input = preprocessImage(resizedBitmap)

        val output = Array(1) { FloatArray(23) } // Adjust if your model has a different output size
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
        val conditionLabels = listOf(
            "Acne", "Actinic Keratosis", "Atopic Dermatitis", "Basal Cell Carcinoma",
            "Benign Keratosis", "Cellulitis", "Dermatofibroma", "Eczema", "Hemangioma",
            "Herpes", "Impetigo", "Lichen Planus", "Melanoma", "Molluscum Contagiosum",
            "Nevus (Mole)", "Psoriasis", "Rosacea", "Scabies", "Seborrheic Keratosis",
            "Shingles (Herpes Zoster)", "Tinea (Ringworm)", "Urticaria (Hives)", "Vitiligo"
        )
        return conditionLabels.getOrElse(index) { "Unknown" }
    }

    private fun getRemedy(condition: String): String {
        return when (condition) {
            "Acne" -> "Keep skin clean, avoid oily products, use benzoyl peroxide or salicylic acid."
            "Actinic Keratosis" -> "See a dermatologist; may require cryotherapy or topical creams."
            "Atopic Dermatitis" -> "Moisturize frequently, avoid harsh soaps, use hydrocortisone cream."
            "Basal Cell Carcinoma" -> "Seek medical treatment immediately; usually requires surgical removal."
            "Benign Keratosis" -> "Usually harmless, but consult a dermatologist for proper evaluation."
            "Cellulitis" -> "Requires antibiotics. Consult a healthcare professional immediately."
            "Dermatofibroma" -> "Harmless, but removal is possible through minor surgery if needed."
            "Eczema" -> "Use moisturizers, avoid triggers, and apply topical steroids if prescribed."
            "Hemangioma" -> "Usually fades over time; seek medical advice if it grows or bleeds."
            "Herpes" -> "Apply antiviral cream, avoid touching lesions, and consult your doctor."
            "Impetigo" -> "Clean the area, use antibiotic ointment, and avoid sharing personal items."
            "Lichen Planus" -> "Topical corticosteroids can help reduce inflammation and itching."
            "Melanoma" -> "Seek urgent medical evaluation; early treatment improves prognosis."
            "Molluscum Contagiosum" -> "Avoid scratching, keep skin clean, lesions may resolve on their own."
            "Nevus (Mole)" -> "Monitor for changes in shape or color; consult a dermatologist for evaluation."
            "Psoriasis" -> "Moisturize often, use medicated creams, and avoid stress or triggers."
            "Rosacea" -> "Avoid spicy food and hot drinks, use gentle skincare, consult your doctor."
            "Scabies" -> "Apply permethrin cream, wash bedding, and treat close contacts."
            "Seborrheic Keratosis" -> "Usually harmless, but can be removed for cosmetic reasons."
            "Shingles (Herpes Zoster)" -> "Apply cool compresses, take antiviral meds, manage pain."
            "Tinea (Ringworm)" -> "Use antifungal cream, keep area dry, avoid sharing clothing."
            "Urticaria (Hives)" -> "Avoid allergens, take antihistamines, and stay cool."
            "Vitiligo" -> "Use sunscreen, consider topical corticosteroids or light therapy."
            else -> "No specific remedy found. Consult a dermatologist for diagnosis and treatment."
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }




}