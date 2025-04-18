package com.example.dermascanai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.format.DateFormat
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemCommentBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterComment(
    private val context: Context,
    private val commentList: List<Comment>
) : RecyclerView.Adapter<AdapterComment.CommentViewHolder>() {

    inner class CommentViewHolder(val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        with(holder.binding) {

            // Set comment content
            textCommentContent.text = comment.comment

            // Format and set the comment timestamp
            val date = DateFormat.format("MMM dd, yyyy hh:mm a", comment.timestamp).toString()
            textCommentTime.text = date

            val userRef = FirebaseDatabase
                .getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("userInfo")
                .child(comment.userId)

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {

                    val fullName = snapshot.child("name").value.toString()
                    val base64Image = snapshot.child("profileImage").value.toString()

                    textUserName.text = fullName

                    val decodedImage = decodeBase64ToBitmap(base64Image)
                    if (decodedImage != null) {
                        imageUser.setImageBitmap(decodedImage)
                    } else {
                        imageUser.setImageResource(R.drawable.ic_profile2)
                    }
                }
            }.addOnFailureListener {
                // Handle failure if the user data could not be fetched
                textUserName.text = "Unknown User"
                imageUser.setImageResource(R.drawable.ic_profile2)  // Fallback to default image
            }
        }
    }

    override fun getItemCount(): Int = commentList.size

    // Function to decode Base64 string to Bitmap
    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedString = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
