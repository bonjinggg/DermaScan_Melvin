package com.example.dermascanai

import android.os.Bundle
import android.view.View
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dermascanai.databinding.ActivityBookingRecordsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class BookingApprovalRecords : AppCompatActivity() {
    private lateinit var binding: ActivityBookingRecordsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: BookingApprovalAdapter
    private val appointmentList = mutableListOf<BookingData>()
    private var clinicName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
        auth = FirebaseAuth.getInstance()

        clinicName = auth.currentUser?.email ?: ""
        if (clinicName.isEmpty()) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchClinicName()

        setupRecyclerView()
        loadPendingAppointments()

        binding.backBTN.setOnClickListener {
            finish()
        }

        binding.pendingFilterChip.setOnClickListener {
            binding.pendingFilterChip.isChecked = true
            binding.allFilterChip.isChecked = false
            binding.approvedFilterChip.isChecked = false
            binding.declinedFilterChip.isChecked = false
            binding.cancelledFilterChip.isChecked = false
            loadPendingAppointments()
        }

        binding.approvedFilterChip.setOnClickListener {
            binding.approvedFilterChip.isChecked = true
            binding.allFilterChip.isChecked = false
            binding.pendingFilterChip.isChecked = false
            binding.declinedFilterChip.isChecked = false
            binding.cancelledFilterChip.isChecked = false
            loadApprovedAppointments()
        }

        binding.declinedFilterChip.setOnClickListener {
            binding.declinedFilterChip.isChecked = true
            binding.allFilterChip.isChecked = false
            binding.pendingFilterChip.isChecked = false
            binding.approvedFilterChip.isChecked = false
            binding.cancelledFilterChip.isChecked = false
            loadDeclineAppointments()
        }

        binding.cancelledFilterChip.setOnClickListener {
            binding.cancelledFilterChip.isChecked = true
            binding.allFilterChip.isChecked = false
            binding.pendingFilterChip.isChecked = false
            binding.approvedFilterChip.isChecked = false
            binding.declinedFilterChip.isChecked = false
            loadCancelledAppointments()
        }

        binding.allFilterChip.setOnClickListener {
            binding.allFilterChip.isChecked = true
            binding.pendingFilterChip.isChecked = false
            binding.approvedFilterChip.isChecked = false
            binding.declinedFilterChip.isChecked = false
            binding.cancelledFilterChip.isChecked = false
            loadAllAppointments()
        }
    }

    private fun fetchClinicName() {
        val clinicRef = database.getReference("clinicName")
            .child(clinicName.replace(".", ","))
            .child("clinicName")

        clinicRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("BookingApprovalRecords", "Bookings found: ${snapshot.childrenCount}")
                if (snapshot.exists()) {
                    for (bookingSnapshot in snapshot.children) {
                        Log.d("BookingApprovalRecords", "Booking key: ${bookingSnapshot.key}")
                        val booking = bookingSnapshot.getValue(BookingData::class.java)
                        if (booking != null) {
                            Log.d("BookingApprovalRecords", "Booking status: ${booking.status}")
                            appointmentList.add(booking)
                        }
                    }
                }
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BookingApprovalRecords, "Error fetching clinic name: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = BookingApprovalAdapter(
            appointmentList,
            onApprove = { booking -> updateBookingStatus(booking, "confirmed") },
            onDecline = { booking -> showDeclineReasonDialog(booking) },
            onCancel = { booking -> showCancellationReasonDialog(booking) }
        )
        binding.bookingRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.bookingRecyclerView.adapter = adapter
    }

    private fun loadPendingAppointments() {
        binding.progressBar.visibility = View.VISIBLE
        appointmentList.clear()

        val doctorBookingsRef = database.getReference("clinicBookings")
            .child(clinicName.replace(".", ","))

        doctorBookingsRef.orderByChild("status").equalTo("pending")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (bookingSnapshot in snapshot.children) {
                            val booking = bookingSnapshot.getValue(BookingData::class.java)
                            booking?.let {
                                appointmentList.add(it)
                            }
                        }
                        appointmentList.sortByDescending { it.timestampMillis }
                        adapter.notifyDataSetChanged()

                        // Show empty state or content
                        updateViewVisibility()
                    } else {
                        appointmentList.clear()
                        adapter.notifyDataSetChanged()
                        updateViewVisibility()
                    }
                    binding.progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BookingApprovalRecords, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun loadApprovedAppointments() {
        binding.progressBar.visibility = View.VISIBLE
        appointmentList.clear()

        val doctorBookingsRef = database.getReference("clinicBookings")
            .child(clinicName.replace(".", ","))

        doctorBookingsRef.orderByChild("status").equalTo("confirmed")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (bookingSnapshot in snapshot.children) {
                            val booking = bookingSnapshot.getValue(BookingData::class.java)
                            booking?.let {
                                appointmentList.add(it)
                            }
                        }
                        // Sort by appointment date/time (descending order - newest first)
                        appointmentList.sortByDescending { it.timestampMillis }
                        adapter.notifyDataSetChanged()

                        // Show empty state or content
                        updateViewVisibility()
                    } else {
                        appointmentList.clear()
                        adapter.notifyDataSetChanged()
                        updateViewVisibility()
                    }
                    binding.progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BookingApprovalRecords, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun loadDeclineAppointments() {
        binding.progressBar.visibility = View.VISIBLE
        appointmentList.clear()

        val doctorBookingsRef = database.getReference("clinicBookings")
            .child(clinicName.replace(".", ","))

        doctorBookingsRef.orderByChild("status").equalTo("declined")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (bookingSnapshot in snapshot.children) {
                            val booking = bookingSnapshot.getValue(BookingData::class.java)
                            booking?.let {
                                appointmentList.add(it)
                            }
                        }
                        // Sort by appointment date/time (descending order - newest first)
                        appointmentList.sortByDescending { it.timestampMillis }
                        adapter.notifyDataSetChanged()

                        // Show empty state or content
                        updateViewVisibility()
                    } else {
                        appointmentList.clear()
                        adapter.notifyDataSetChanged()
                        updateViewVisibility()
                    }
                    binding.progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BookingApprovalRecords, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun loadCancelledAppointments() {
        binding.progressBar.visibility = View.VISIBLE
        appointmentList.clear()

        val doctorBookingsRef = database.getReference("clinicBookings")
            .child(clinicName.replace(".", ","))

        doctorBookingsRef.orderByChild("status").equalTo("cancelled")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (bookingSnapshot in snapshot.children) {
                            val booking = bookingSnapshot.getValue(BookingData::class.java)
                            booking?.let {
                                appointmentList.add(it)
                            }
                        }
                        // Sort by appointment date/time (descending order - newest first)
                        appointmentList.sortByDescending { it.timestampMillis }
                        adapter.notifyDataSetChanged()

                        // Show empty state or content
                        updateViewVisibility()
                    } else {
                        appointmentList.clear()
                        adapter.notifyDataSetChanged()
                        updateViewVisibility()
                    }
                    binding.progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BookingApprovalRecords, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun loadAllAppointments() {
        binding.progressBar.visibility = View.VISIBLE
        appointmentList.clear()

        val doctorBookingsRef = database.getReference("clinicBookings")
            .child(clinicName.replace(".", ","))

        doctorBookingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (bookingSnapshot in snapshot.children) {
                        val booking = bookingSnapshot.getValue(BookingData::class.java)
                        booking?.let {
                            appointmentList.add(it)
                        }
                    }
                    // Sort by appointment date/time (descending order - newest first)
                    appointmentList.sortByDescending { it.timestampMillis }
                    adapter.notifyDataSetChanged()
                    updateViewVisibility()
                } else {
                    appointmentList.clear()
                    adapter.notifyDataSetChanged()
                    updateViewVisibility()
                }
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BookingApprovalRecords, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateViewVisibility() {
        if (appointmentList.isEmpty()) {
            // Show appropriate empty state based on selected filter
            when {
                binding.pendingFilterChip.isChecked -> {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.emptyStateDeclinedLayout.visibility = View.GONE
                    binding.emptyStateApprovedLayout.visibility = View.GONE
                    binding.emptyStateCancelledLayout.visibility = View.GONE
                }
                binding.declinedFilterChip.isChecked -> {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.emptyStateDeclinedLayout.visibility = View.VISIBLE
                    binding.emptyStateApprovedLayout.visibility = View.GONE
                    binding.emptyStateCancelledLayout.visibility = View.GONE
                }
                binding.approvedFilterChip.isChecked -> {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.emptyStateDeclinedLayout.visibility = View.GONE
                    binding.emptyStateApprovedLayout.visibility = View.VISIBLE
                    binding.emptyStateCancelledLayout.visibility = View.GONE
                }
                binding.cancelledFilterChip.isChecked -> {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.emptyStateDeclinedLayout.visibility = View.GONE
                    binding.emptyStateApprovedLayout.visibility = View.GONE
                    binding.emptyStateCancelledLayout.visibility = View.VISIBLE
                }
                else -> {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.emptyStateDeclinedLayout.visibility = View.GONE
                    binding.emptyStateApprovedLayout.visibility = View.GONE
                    binding.emptyStateCancelledLayout.visibility = View.GONE
                }
            }
            binding.bookingRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.emptyStateDeclinedLayout.visibility = View.GONE
            binding.emptyStateApprovedLayout.visibility = View.GONE
            binding.emptyStateCancelledLayout.visibility = View.GONE
            binding.bookingRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateBookingStatus(booking: BookingData, newStatus: String) {
        binding.progressBar.visibility = View.VISIBLE

        // Update status in all three locations
        val updates = HashMap<String, Any>()

        // Main bookings reference
        updates["/bookings/${booking.bookingId}/status"] = newStatus

        // Clinic bookings reference
        updates["/clinicBookings/${clinicName.replace(".", ",")}" +
                "/${booking.bookingId}/status"] = newStatus

        // User bookings reference
        updates["/userBookings/${booking.patientEmail.replace(".", ",")}" +
                "/${booking.bookingId}/status"] = newStatus

        // Add clinic name if available
        if (clinicName.isNotEmpty()) {
            updates["/bookings/${booking.bookingId}/clinicName"] = clinicName
            updates["/clinicBookings/${clinicName.replace(".", ",")}" +
                    "/${booking.bookingId}/clinicName"] = clinicName
            updates["/userBookings/${booking.patientEmail.replace(".", ",")}" +
                    "/${booking.bookingId}/clinicName"] = clinicName
        }

        // Add reason if it's a decline or cancellation
        if (booking.declineReason != null && booking.declineReason!!.isNotEmpty()) {
            updates["/bookings/${booking.bookingId}/declineReason"] = booking.declineReason!!
            updates["/clinicBookings/${clinicName.replace(".", ",")}" +
                    "/${booking.bookingId}/declineReason"] = booking.declineReason!!
            updates["/userBookings/${booking.patientEmail.replace(".", ",")}" +
                    "/${booking.bookingId}/declineReason"] = booking.declineReason!!
        }

        // Add cancellation reason if present
        if (booking.cancellationReason != null && booking.cancellationReason!!.isNotEmpty()) {
            updates["/bookings/${booking.bookingId}/cancellationReason"] = booking.cancellationReason!!
            updates["/clinicBookings/${clinicName.replace(".", ",")}" +
                    "/${booking.bookingId}/cancellationReason"] = booking.cancellationReason!!
            updates["/userBookings/${booking.patientEmail.replace(".", ",")}" +
                    "/${booking.bookingId}/cancellationReason"] = booking.cancellationReason!!
        }

        // Apply all updates atomically
        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                val statusMessage = when (newStatus) {
                    "confirmed" -> "approved"
                    "cancelled" -> "cancelled"
                    else -> "declined"
                }

                Toast.makeText(
                    this,
                    "Appointment $statusMessage",
                    Toast.LENGTH_SHORT
                ).show()

                // Refresh the list based on current filter
                refreshCurrentView()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun refreshCurrentView() {
        when {
            binding.pendingFilterChip.isChecked -> loadPendingAppointments()
            binding.approvedFilterChip.isChecked -> loadApprovedAppointments()
            binding.declinedFilterChip.isChecked -> loadDeclineAppointments()
            binding.cancelledFilterChip.isChecked -> loadCancelledAppointments()
            else -> loadAllAppointments()
        }
    }

    private fun showDeclineReasonDialog(booking: BookingData) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Decline Appointment")
        builder.setMessage("Please provide a reason for declining (optional):")

        val input = androidx.appcompat.widget.AppCompatEditText(this)
        builder.setView(input)

        builder.setPositiveButton("Decline") { _, _ ->
            val reason = input.text.toString().trim()
            if (reason.isNotEmpty()) {
                booking.declineReason = reason
            }
            updateBookingStatus(booking, "declined")
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showCancellationReasonDialog(booking: BookingData) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cancel Appointment")
        builder.setMessage("Please provide a reason for cancellation:")

        val input = androidx.appcompat.widget.AppCompatEditText(this)
        builder.setView(input)

        builder.setPositiveButton("Cancel Appointment") { _, _ ->
            val reason = input.text.toString().trim()
            if (reason.isNotEmpty()) {
                booking.cancellationReason = reason
                updateBookingStatus(booking, "cancelled")
            } else {
                Toast.makeText(this, "Please provide a cancellation reason", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Back") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}