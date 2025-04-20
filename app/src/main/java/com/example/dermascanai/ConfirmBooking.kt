package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityConfirmBookingBinding

class ConfirmBooking : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmBookingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivityConfirmBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBTN.setOnClickListener {
            finish()
        }

        binding.confirm.setOnClickListener {
            val intent = Intent(this, UserPage::class.java)
            startActivity(intent)
            finish()
        }

    }
}