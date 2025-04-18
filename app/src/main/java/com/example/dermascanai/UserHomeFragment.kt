package com.example.dermascanai

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.dermascanai.databinding.FragmentHomeUserBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class UserHomeFragment : Fragment() {
    private var _binding: FragmentHomeUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var mDatabase: DatabaseReference
    private lateinit var mAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeUserBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a")
        val formatted = current.format(formatter)

        val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
        val navView = requireActivity().findViewById<NavigationView>(R.id.navigationView)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("userInfo")

        val headerView = navView.getHeaderView(0)
        val closeDrawerBtn = headerView.findViewById<ImageView>(R.id.closeDrawerBtn)

        binding.dateTimeText.text = formatted

        val userId = mAuth.currentUser?.uid

        if (userId != null) {
            getUserData(userId)
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        binding.doctorList.setOnClickListener {
            val intent = Intent(requireContext(), DoctorLists::class.java)
            startActivity(intent)
        }


        binding.menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        closeDrawerBtn.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    Toast.makeText(context, "Settings Clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_update_profile -> {
                    Toast.makeText(context, "Update Profile Clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    logoutUser()

                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getUserData(userId: String) {
        val userRef = mDatabase.child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(UserInfo::class.java)

                if (user != null) {
                    binding.fullName.text = "${user.name}"

                    if (user.profileImage != null) {
                        try {
                            val decodedByteArray = Base64.decode(user.profileImage, Base64.DEFAULT)
                            val decodedBitmap = BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)

                            binding.profileView.setImageBitmap(decodedBitmap)
                        } catch (e: Exception) {
                            Log.e("UserProfileFragment", "Error decoding Base64 image", e)
                            Toast.makeText(context, "Error loading profile image", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Glide.with(this)
                            .load(R.drawable.ic_profile)
                            .into(binding.profileView)
                    }

                } else {
                    Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "User not found in database", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("UserProfileFragment", "Error fetching user data", e)
            Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
        }
    }


    private fun logoutUser() {
        val builder = AlertDialog.Builder(requireContext())
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