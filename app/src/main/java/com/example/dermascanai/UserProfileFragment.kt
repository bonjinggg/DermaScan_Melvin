package com.example.dermascanai

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.palette.graphics.Palette
import com.example.dermascanai.databinding.FragmentProfileUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class UserProfileFragment : Fragment() {
    private var _binding: FragmentProfileUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        fetchUserData()
        val imageView = binding.bannerImg

        imageView.drawable?.let { drawable ->
            val bitmap = (drawable as BitmapDrawable).bitmap
            Palette.from(bitmap).generate { palette ->
                palette?.let {
                    val dominantColor = it.getDominantColor(Color.WHITE)
                    val isDark = isColorDark(dominantColor)
                    val textColor = if (isDark) Color.WHITE else Color.BLACK
                    binding.fullName.setTextColor(textColor)
                    binding.quote.setTextColor(textColor)

                }
            }
        }

        binding.logout.setOnClickListener {
            logoutUser()
        }

        binding.btnToggleInfo.setOnClickListener {
        val intent = Intent(requireContext(), PersonalInfo::class.java)
        startActivity(intent)
        }
        binding.editBannerIcon.setOnClickListener {
            val banners = listOf(
                R.drawable.banner1,
                R.drawable.banner2,
                R.drawable.banner3,
                R.drawable.banner4,
                R.drawable.banner5,
                R.drawable.banner6,
                R.drawable.banner7,
                R.drawable.banner8,
                R.drawable.banner9,
                R.drawable.banner10,
                R.drawable.banner11,
                R.drawable.banner12

            )

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Choose a Banner")
                .setItems(arrayOf("Banner 1", "Banner 2", "Banner 3", "Banner 4", "Banner 5", "Banner 6", "Banner 7", "Banner 8", "Banner 9", "Banner 10", "Banner 11", "Banner 12")) { _, which ->
                    binding.bannerImg.setImageResource(banners[which])
                }
                .create()

            dialog.show()
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun fetchUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val userRef: DatabaseReference = database.getReference("userInfo").child(userId ?: return)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val userInfo = snapshot.getValue(UserInfo::class.java)

                // Set name
                binding.fullName.setText(userInfo?.name ?: "")
                val text = (userInfo?.quote ?: "")
                binding.quote.text = "\" $text \""
                // Set profile image if available
                userInfo?.profileImage?.let {
                    if (it.isNotEmpty()) {
                        val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        binding.profPic.setImageBitmap(bitmap)
                    }
                }
            } else {
//                Toast.makeText(this, "No user data found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
//            Toast.makeText(this, "Failed to fetch user data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logoutUser() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireActivity(), Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

}