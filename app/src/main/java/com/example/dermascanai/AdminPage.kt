package com.example.dermascanai

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityAdminPageBinding

class AdminPage : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPageBinding
    private var isFabMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navHome.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, UserHomeFragment())
                .commit()
                closeFabMenu()
        }

        binding.navProfile.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, UserProfileFragment())
                .commit()
            closeFabMenu()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, UserHomeFragment())
            .commit()



        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabScan.setOnClickListener {
            Toast.makeText(this, "Scan Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.fabBlog.setOnClickListener {
            Toast.makeText(this, "Blog Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.navHome.setOnClickListener {
            Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.navProfile.setOnClickListener {
            Toast.makeText(this, "Profile Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFabMenu() {
        if (!isFabMenuOpen) {
            binding.fabScan.visibility = View.VISIBLE
            binding.fabBlog.visibility = View.VISIBLE
            binding.fabMain.setImageResource(R.drawable.ic_expandd)
        } else {
            binding.fabScan.visibility = View.GONE
            binding.fabBlog.visibility = View.GONE
            binding.fabMain.setImageResource(R.drawable.ic_expandu)
        }
        isFabMenuOpen = !isFabMenuOpen
    }
    private fun closeFabMenu() {
        binding.fabScan.visibility = View.GONE
        binding.fabBlog.visibility = View.GONE
        binding.fabMain.setImageResource(R.drawable.ic_expandu)
        isFabMenuOpen = false
    }

}