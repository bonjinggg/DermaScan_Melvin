package com.example.dermascanai

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class PostBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var editTextPostContent: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var buttonUploadImage: Button
    private lateinit var buttonPost: Button

    private var base64Image: String? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.bottom_post_modal, container, false)
        editTextPostContent = view.findViewById(R.id.editTextPostContent)
        imagePreview = view.findViewById(R.id.imagePreview)
        buttonUploadImage = view.findViewById(R.id.buttonUploadImage)
        buttonPost = view.findViewById(R.id.buttonPost)

        buttonUploadImage.setOnClickListener { openGallery() }

        buttonPost.setOnClickListener {
            val content = editTextPostContent.text.toString()
            if (content.isNotBlank()) {
                savePostToFirebase(content, base64Image)
                dismiss()
            } else {
                Toast.makeText(context, "Content cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                imagePreview.setImageURI(imageUri)
                imagePreview.visibility = View.VISIBLE
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                base64Image = encodeImageToBase64(bitmap)
            }
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun savePostToFirebase(content: String, imageBase64: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        val postRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("Posts").push()

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fullName = snapshot.child("fullName").value.toString()
                val profilePic = snapshot.child("profileImage").value.toString() // assumed Base64

                val postData = mapOf(
                    "userId" to userId,
                    "fullName" to fullName,
                    "profilePic" to profilePic,
                    "content" to content,
                    "imagePost" to (imageBase64 ?: ""),
                    "timestamp" to System.currentTimeMillis()
                )
                postRef.setValue(postData)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
