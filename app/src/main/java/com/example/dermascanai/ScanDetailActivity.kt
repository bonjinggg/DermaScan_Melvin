package com.example.dermascanai

import android.os.Bundle
import android.util.Base64
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dermascanai.databinding.ActivityScanDetailBinding
import com.google.firebase.database.*

class ScanDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanDetailBinding
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get userId and scanId from intent
        val userId = intent.getStringExtra("userId")
        val scanId = intent.getStringExtra("scanId")

        if (userId.isNullOrEmpty() || scanId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid scan data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Firebase path: scanResults/userId/scanId
        databaseRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("scanResults")
            .child(userId)
            .child(scanId)

        fetchScanDetails()
    }

    private fun fetchScanDetails() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val condition = snapshot.child("condition").getValue(String::class.java) ?: "Unknown"
                    val remedy = snapshot.child("remedy").getValue(String::class.java) ?: "Not available"
                    val timestamp = snapshot.child("timestamp").getValue(String::class.java) ?: "Unknown"
                    val imageBase64 = snapshot.child("imageBase64").getValue(String::class.java)

                    binding.textViewConditionDetail.text = "Condition: $condition"
                    binding.textViewRemedyDetail.text = "Remedy: $remedy"
                    binding.textViewTimestampDetail.text = "Timestamp: $timestamp"

                    imageBase64?.let {
                        try {
                            val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            binding.imageViewDetail.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            binding.imageViewDetail.setImageResource(R.drawable.ic_scan)
                        }
                    } ?: run {
                        binding.imageViewDetail.setImageResource(R.drawable.ic_scan)
                    }

                } else {
                    Toast.makeText(this@ScanDetailActivity, "Scan data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScanDetailActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
