package com.example.dermascanai

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityDiseaseDetailsBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DiseaseDetails : AppCompatActivity() {
    private lateinit var binding: ActivityDiseaseDetailsBinding
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiseaseDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        fetchDetails()
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun fetchDetails(){
        val condition = intent.getStringExtra("condition")
        val img = intent.getStringExtra("img")
        val diseaseRef: DatabaseReference = database.getReference("disease").child(condition ?: return)

        diseaseRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val diseaseInfo = snapshot.getValue(DiseaseInfo::class.java)

                binding.dataImg.setImageBitmap(img as Bitmap?)
                binding.textView22.setText(condition)
                binding.diseaseDescription.text = diseaseInfo?.des
                binding.diseaseCause.text = diseaseInfo?.cause
                binding.diseaseRemedy.text = diseaseInfo?.rem
                binding.diseasePrevention.text = diseaseInfo?.prev
            }
        }
    }
}