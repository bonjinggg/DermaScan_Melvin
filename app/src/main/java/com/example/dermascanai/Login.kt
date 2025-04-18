package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError


class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
        mDatabase = database.getReference("userInfo")

        val currentUser = mAuth.currentUser
        if (currentUser != null){

            redirectToRolePage()
        }

        binding.loginButton.setOnClickListener {
            loginUser()
        }

        binding.forgotPassword.setOnClickListener {
            forgotPassword()
        }
        binding.toRegister.setOnClickListener {
            val intent = Intent(this, ChooseUser::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress or loading animation here
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    redirectToRolePage()  // Redirection based on the role after successful login
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun redirectToRolePage() {
        val userId = mAuth.currentUser?.uid
        if (userId != null) {
            mDatabase.child(userId).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val role = dataSnapshot.getValue(String::class.java)
                        Log.d("LoginActivity", "User role: $role")

                        if (role != null) {
                            val intent = when (role) {
                                "dermatologist" -> Intent(this@Login, DermaPage::class.java)
                                "user" -> Intent(this@Login, UserPage::class.java)
                                "admin" -> Intent(this@Login, AdminPage::class.java)
                                else -> {
                                    Toast.makeText(this@Login, "Unknown role", Toast.LENGTH_SHORT).show()
                                    null
                                }
                            }
                            intent?.let {
                                startActivity(it)
                                finish()  // Close login screen after successful redirection
                            }
                        } else {
                            Toast.makeText(this@Login, "No role found for this user", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@Login, "Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun forgotPassword() {
        val email = binding.email.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}