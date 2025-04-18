package com.example.dermascanai

import android.os.Bundle
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dermaList = mutableListOf<User>()

        val databaseRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users")

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dermaList.clear()
                var count = 0
                for (userSnap in snapshot.children) {
                    val user = userSnap.getValue(User::class.java)
                    if (user != null) {
                        println("User found: ${user.fullName}, role: ${user.role}")
                        if (user.role.lowercase() == "derma") {
                            dermaList.add(user)
                            count++
                        }
                    }
                }
                Toast.makeText(this@DoctorLists, "$count dermatologists found", Toast.LENGTH_SHORT).show()
                binding.recyclerView.adapter = AdapterDoctorList(dermaList)
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DoctorLists, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
        })
    }
}