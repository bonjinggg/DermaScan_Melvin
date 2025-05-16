package com.example.dermascanai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ActivityBookingHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class BookingHistory : AppCompatActivity() {
    private lateinit var binding: ActivityBookingHistoryBinding
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val appointmentList = mutableListOf<Appointment>()
    private val filteredAppointmentList = mutableListOf<Appointment>()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userBookingsRef: DatabaseReference
    private var userBookingsListener: ValueEventListener? = null

    // Current filter
    private var currentFilter = "pending"

    // Database caching and offline support
    private var isDatabaseInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase components
        initializeFirebase()

        setupRecyclerView()
        setupFilterChips()

        binding.backBtn.setOnClickListener {
            finish()
        }

        // Initial loading of appointments
        loadAppointments()

        // Set up auto-refresh
        setupAutoRefresh()
    }

    private fun initializeFirebase() {
        if (!isDatabaseInitialized) {
            auth = FirebaseAuth.getInstance()

            // Initialize database with persistence for offline support
            database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

            // Enable disk persistence (offline capabilities)
            try {
                database.setPersistenceEnabled(true)
                Log.d("BookingHistory", "Firebase persistence enabled")
            } catch (e: Exception) {
                Log.e("BookingHistory", "Firebase persistence already set: ${e.message}")
            }

            isDatabaseInitialized = true
        }
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(filteredAppointmentList)
        binding.appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appointmentsRecyclerView.adapter = appointmentAdapter
    }

    private fun setupFilterChips() {
        // Set default chip checked
        binding.pendingFilterChip.isChecked = true

        // Set up click listeners for filter chips
        binding.pendingFilterChip.setOnClickListener {
            currentFilter = "pending"
            applyFilter()
        }

        binding.declinedFilterChip.setOnClickListener {
            currentFilter = "declined"
            applyFilter()
        }

        binding.approvedFilterChip.setOnClickListener {
            currentFilter = "approved"
            applyFilter()
        }

        binding.ongoingFilterChip.setOnClickListener {
            currentFilter = "ongoing"
            applyFilter()
        }

        binding.allFilterChip.setOnClickListener {
            currentFilter = "all"
            applyFilter()
        }
    }

    private fun applyFilter() {
        filteredAppointmentList.clear()

        // Hide all empty state layouts first
        binding.emptyStateLayout.visibility = View.GONE
        binding.emptyStateDeclinedLayout.visibility = View.GONE
        binding.emptyStateApprovedLayout.visibility = View.GONE

        when (currentFilter) {
            "pending" -> {
                filteredAppointmentList.addAll(appointmentList.filter {
                    it.status.lowercase() == "pending"
                })

                if (filteredAppointmentList.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                }
            }
            "declined" -> {
                filteredAppointmentList.addAll(appointmentList.filter {
                    it.status.lowercase() == "declined" || it.status.lowercase() == "cancelled"
                })

                if (filteredAppointmentList.isEmpty()) {
                    binding.emptyStateDeclinedLayout.visibility = View.VISIBLE
                }
            }
            "approved" -> {
                filteredAppointmentList.addAll(appointmentList.filter {
                    it.status.lowercase() == "confirmed" || it.status.lowercase() == "completed"
                })

                if (filteredAppointmentList.isEmpty()) {
                    binding.emptyStateApprovedLayout.visibility = View.VISIBLE
                }
            }
            "ongoing" -> {
                filteredAppointmentList.addAll(appointmentList.filter {
                    it.status.lowercase() == "ongoing"
                })

                if (filteredAppointmentList.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                }
            }
            "all" -> {
                filteredAppointmentList.addAll(appointmentList)

                if (filteredAppointmentList.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                }
            }
        }

        appointmentAdapter.notifyDataSetChanged()
        Log.d("BookingHistory", "Applied filter: $currentFilter, showing ${filteredAppointmentList.size} appointments")
    }

    private fun loadAppointments() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "Please login to view your appointments", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        hideAllEmptyStates()

        // Get current user email and format it for Firebase path
        val userEmail = currentUser.email?.replace(".", ",") ?: ""
        Log.d("BookingHistory", "Loading appointments for user: $userEmail")

        // Clean up previous listener if it exists
        if (userBookingsListener != null && ::userBookingsRef.isInitialized) {
            userBookingsRef.removeEventListener(userBookingsListener!!)
        }

        // Set up the database reference with caching
        userBookingsRef = database.getReference("userBookings").child(userEmail)

        // Configure for better performance
        userBookingsRef.keepSynced(true) // Keep this data synced locally

        userBookingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                appointmentList.clear()

                if (snapshot.exists()) {
                    for (bookingSnapshot in snapshot.children) {
                        val appointment = bookingSnapshot.getValue(Appointment::class.java)
                        if (appointment != null) {
                            appointmentList.add(appointment)
                            Log.d("BookingHistory", "Found appointment: ${appointment.bookingId} with doctor ${appointment.doctorName}, status: ${appointment.status}")
                        }
                    }

                    // Sort appointments by timestamp (most recent first)
                    appointmentList.sortByDescending { it.timestampMillis }
                } else {
                    Log.d("BookingHistory", "No appointments found for user: $userEmail")
                }

                // Apply current filter to the loaded data
                applyFilter()

                // Update UI
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BookingHistory, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                Log.e("BookingHistory", "Database error: ${error.message}")
            }
        }

        // Add the listener for real-time updates
        userBookingsRef.addValueEventListener(userBookingsListener!!)
    }

    private fun hideAllEmptyStates() {
        binding.emptyStateLayout.visibility = View.GONE
        binding.emptyStateDeclinedLayout.visibility = View.GONE
        binding.emptyStateApprovedLayout.visibility = View.GONE
    }

    // Auto-refresh timer
    private var autoRefreshRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val AUTO_REFRESH_INTERVAL = 30000L // 30 seconds

    private fun setupAutoRefresh() {
        autoRefreshRunnable = object : Runnable {
            override fun run() {
                Log.d("BookingHistory", "Auto-refresh triggered")
                // Instead of reloading everything, just check for connection status
                updateConnectionStatus()
                handler.postDelayed(this, AUTO_REFRESH_INTERVAL)
            }
        }
        handler.postDelayed(autoRefreshRunnable!!, AUTO_REFRESH_INTERVAL)
    }

    private fun updateConnectionStatus() {
        val connectedRef = database.getReference(".info/connected")
        connectedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d("BookingHistory", "Connected to Firebase")
                } else {
                    Log.d("BookingHistory", "Disconnected from Firebase")
                    Toast.makeText(this@BookingHistory, "Working offline. Pull to refresh when online.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookingHistory", "Connection check error: ${error.message}")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (::userBookingsRef.isInitialized && userBookingsListener == null) {
            loadAppointments()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        if (userBookingsListener != null && ::userBookingsRef.isInitialized) {
            userBookingsRef.removeEventListener(userBookingsListener!!)
            userBookingsListener = null
        }

        // Remove auto-refresh callback
        autoRefreshRunnable?.let { handler.removeCallbacks(it) }
    }

    inner class AppointmentAdapter(private val appointments: List<Appointment>) :
        RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

        inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val doctorName: TextView = itemView.findViewById(R.id.textDoctorName)
            val appointmentDate: TextView = itemView.findViewById(R.id.textAppointmentDate)
            val appointmentTime: TextView = itemView.findViewById(R.id.textAppointmentTime)
            val appointmentStatus: TextView = itemView.findViewById(R.id.textStatus)
            val messageText: TextView = itemView.findViewById(R.id.textMessage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false)
            return AppointmentViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
            val appointment = appointments[position]

            holder.doctorName.text = appointment.doctorName
            holder.appointmentDate.text = appointment.date
            holder.appointmentTime.text = appointment.time

            val status = appointment.status.replaceFirstChar { it.uppercase() }
            holder.appointmentStatus.text = status

            val color = when (appointment.status.lowercase()) {
                "confirmed" -> getColor(R.color.green)
                "declined", "cancelled" -> getColor(R.color.red)
                "completed" -> getColor(R.color.blue)
                "ongoing" -> getColor(R.color.green)
                else -> getColor(R.color.orange)
            }
            holder.appointmentStatus.setTextColor(color)

            if (appointment.message.isNotEmpty()) {
                holder.messageText.visibility = View.VISIBLE
                holder.messageText.text = "Message: ${appointment.message}"
            } else {
                holder.messageText.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = appointments.size
    }
}