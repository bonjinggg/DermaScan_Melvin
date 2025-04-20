package com.example.dermascanai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityDermaRecordsBinding

class DermaRecords : AppCompatActivity() {
    private lateinit var binding: ActivityDermaRecordsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDermaRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding. backBtn.setOnClickListener {
            finish()
        }
    }
}