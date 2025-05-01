package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityScanRecordsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ScanRecords : AppCompatActivity() {
    private lateinit var binding: ActivityScanRecordsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val scanList = mutableListOf<Pair<String, ScanResult>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.recyclerViewScanResult.layoutManager = LinearLayoutManager(this)

        loadScanResults()
    }

    private fun loadScanResults() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val scanResultsRef = database.reference.child("scanResults").child(userId)

        scanResultsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scanList.clear()
                var count = 0

                for (scanSnapshot in snapshot.children) {
                    val scanResult = scanSnapshot.getValue(ScanResult::class.java)
                    val scanId = scanSnapshot.key
                    if (scanResult != null && scanId != null) {
                        scanList.add(scanId to scanResult)
                        count++
                    }
                }

                Toast.makeText(this@ScanRecords, "$count scan results found", Toast.LENGTH_SHORT).show()

                // ✅ Now only pass scanId and userId to the detail activity
                binding.recyclerViewScanResult.adapter = AdapterScanResult(scanList) { scanId, _ ->
                    val intent = Intent(this@ScanRecords, ScanDetailActivity::class.java).apply {
                        putExtra("scanId", scanId)
                        putExtra("userId", userId)
                    }
                    startActivity(intent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScanRecords, "Failed to load scan results", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
