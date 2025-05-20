package com.example.dermascanai

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityDoctorListsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DoctorLists : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorListsBinding
    private val TAG = "DoctorLists"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dermaList = mutableListOf<ClinicInfo>()

        // Database reference to the clinicInfo node
        val databaseRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("clinicInfo")

        binding.backBTN.setOnClickListener {
            finish()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)


        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dermaList.clear()
                var count = 0

                // Check if data exists at this path
                if (!snapshot.exists()) {
                    Log.e(TAG, "No data found at clinicInfo path")
                    Toast.makeText(this@DoctorLists, "No clinic data available", Toast.LENGTH_SHORT).show()

                    return
                }

                for (userSnap in snapshot.children) {
                    try {
                        val user = userSnap.getValue(ClinicInfo::class.java)
                        if (user != null) {
                            Log.d(TAG, "User found: ${user.name}, role: ${user.role}")
                            if (user.role.lowercase() == "derma") {
                                dermaList.add(user)
                                count++
                            }
                        } else {
                            Log.w(TAG, "Failed to parse user data at ${userSnap.key}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing clinic data for key ${userSnap.key}", e)
                    }
                }


                if (count > 0) {
                    Toast.makeText(this@DoctorLists, "$count dermatologists found", Toast.LENGTH_SHORT).show()
                    binding.recyclerView.adapter = AdapterDoctorList(dermaList)
                } else {
                    Toast.makeText(this@DoctorLists, "No dermatologists found", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(this@DoctorLists, "Failed to load dermatologists: ${error.message}", Toast.LENGTH_SHORT).show()

            }
        })
    }

    private fun tryFallbackToOldPath() {
        val oldDatabaseRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("dermaInfo")

        Log.d(TAG, "Trying fallback to dermaInfo path")

    }
}