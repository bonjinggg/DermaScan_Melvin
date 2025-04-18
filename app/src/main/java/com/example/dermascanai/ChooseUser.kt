package com.example.dermascanai

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityChooseUserBinding
import androidx.core.view.isGone

class ChooseUser : AppCompatActivity() {
    private lateinit var binding: ActivityChooseUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dermatologistCard.setOnClickListener {
            if (binding.expandedContent.isGone) {
                binding.expandedContent.visibility = View.VISIBLE
                binding.dermatologistCard.visibility = View.GONE
                binding.userCard.visibility = View.GONE
            } else {
                binding.expandedContent.visibility = View.GONE
                binding.dermatologistCard.visibility = View.VISIBLE
                binding.userCard.visibility = View.VISIBLE
            }
        }

        binding.userCard.setOnClickListener {
            if (binding.expandedContentU.isGone) {
                binding.expandedContentU.visibility = View.VISIBLE
                binding.dermatologistCard.visibility = View.GONE
                binding.userCard.visibility = View.GONE
            } else {
                binding.expandedContentU.visibility = View.GONE
                binding.dermatologistCard.visibility = View.VISIBLE
                binding.userCard.visibility = View.VISIBLE
            }
        }

        binding.closeButtonD.setOnClickListener {
            binding.expandedContent.visibility = View.GONE
            binding.dermatologistCard.visibility = View.VISIBLE
            binding.userCard.visibility = View.VISIBLE
        }

        binding.closeButtonU.setOnClickListener {
            binding.expandedContentU.visibility = View.GONE
            binding.dermatologistCard.visibility = View.VISIBLE
            binding.userCard.visibility = View.VISIBLE
        }

        binding.dermaBtn.setOnClickListener {
            navigateToRegister("derma")
        }

        binding.userBtn.setOnClickListener {
            navigateToRegister("user")
        }
    }

    private fun navigateToRegister(role: String) {
        val intent = Intent(this, Register::class.java)
        intent.putExtra("USER_ROLE", role)
        startActivity(intent)
    }
}