package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityDermaDetailsBinding

class DermaDetails : AppCompatActivity() {
    private lateinit var binding: ActivityDermaDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivityDermaDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.appointmentBtn.setOnClickListener {
            val intent = Intent(this, Booking::class.java)
            startActivity(intent)
        }

        binding.backBTN.setOnClickListener {
            finish()
        }


    }
}