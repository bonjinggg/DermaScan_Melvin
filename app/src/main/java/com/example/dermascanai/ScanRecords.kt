package com.example.dermascanai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityScanRecordsBinding

class ScanRecords : AppCompatActivity() {
    private lateinit var binding: ActivityScanRecordsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding. backBtn.setOnClickListener {
            finish()
        }
    }
}