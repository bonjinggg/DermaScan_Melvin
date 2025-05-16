package com.example.dermascanai

import android.content.Intent
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dermascanai.databinding.ActivityBookingBinding
import com.google.firebase.database.*
import java.util.Date
import java.util.Locale

class Booking : AppCompatActivity() {
    private lateinit var binding: ActivityBookingBinding
    private var selectedTimeSlot: Button? = null
    private var selectedDate: Long = 0L
    private var selectedTimeText: String = ""
    private var patientEmail: String = ""
    private var doctorEmail: String = ""
    private lateinit var database: FirebaseDatabase
    private lateinit var bookingsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
        bookingsRef = database.getReference("bookings")

        doctorEmail = intent.getStringExtra("doctorEmail") ?: ""
        patientEmail = intent.getStringExtra("patientEmail") ?: ""

        // Check if user already has an active booking before allowing new ones
        checkExistingBooking(patientEmail)

        setupCalendar()

        binding.backBTN.setOnClickListener {
            finish()
        }

        // Setup date selection listener
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            handleDateSelection(year, month, dayOfMonth)
        }

        // Setup time slot buttons
        setupTimeSlotButtons()

        // Setup confirm button
        binding.btnConfirm.setOnClickListener {
            // Double-check for existing bookings before proceeding
            verifyNoExistingBookingsAndProceed()
        }
    }

    private fun setupCalendar() {
        // Set up calendar appearance
        binding.calendarView.setWeekSeparatorLineColor(Color.BLACK)
        binding.calendarView.setFocusedMonthDateColor(Color.BLACK)
        binding.calendarView.setUnfocusedMonthDateColor(Color.BLACK)

        // Get current date and move to start of tomorrow
        val tomorrow = Calendar.getInstance()
        tomorrow.set(Calendar.HOUR_OF_DAY, 0)
        tomorrow.set(Calendar.MINUTE, 0)
        tomorrow.set(Calendar.SECOND, 0)
        tomorrow.set(Calendar.MILLISECOND, 0)
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)

        // Set minimum date to tomorrow
        binding.calendarView.minDate = tomorrow.timeInMillis
    }


    private fun handleDateSelection(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        selectedDate = calendar.timeInMillis

        // Reset time slots when a new date is chosen
        resetTimeSlots()

        // Check for available time slots
        fetchBookedTimeSlots(calendar.timeInMillis)
    }

    private fun setupTimeSlotButtons() {
        val timeButtons = listOf(
            binding.btn910, binding.btn1011, binding.btn1112,
            binding.btn12, binding.btn23, binding.btn34, binding.btn45
        )

        for (btn in timeButtons) {
            btn.setOnClickListener {
                if (btn.isEnabled) {
                    selectedTimeSlot?.setBackgroundColor(Color.parseColor("#7A7A7A"))
                    btn.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                    selectedTimeSlot = btn
                    selectedTimeText = btn.text.toString()
                }
            }
        }
    }

    private fun verifyNoExistingBookingsAndProceed() {
        if (selectedDate == 0L || selectedTimeText.isEmpty()) {
            Toast.makeText(this, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
            return
        }

        // Final check to ensure user doesn't have any active bookings
        bookingsRef.orderByChild("patientEmail").equalTo(patientEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasActiveBooking = false
                    var bookingStatus = ""

                    for (bookingSnapshot in snapshot.children) {
                        val status = bookingSnapshot.child("status").getValue(String::class.java)

                        // Check for "Pending" or "Confirmed" status bookings
                        if (status == "Pending" || status == "Confirmed") {
                            hasActiveBooking = true
                            bookingStatus = status
                            break
                        }
                    }

                    if (hasActiveBooking) {
                        AlertDialog.Builder(this@Booking)
                            .setTitle("Booking Already Exists")
                            .setMessage("You already have a $bookingStatus appointment. Please cancel it before booking a new one.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    } else {
                        // Proceed with booking
                        proceedWithBooking()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@Booking,
                        "Error checking bookings: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun proceedWithBooking() {
        // Prepare a booking ID using timestamp for uniqueness
        val bookingId = "${System.currentTimeMillis()}"

        // Format the date for display
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(selectedDate))

        // Format the time for display
        val formattedTime = formatTimeSlot(selectedTimeText)

        // Pass to next activity
        val intent = Intent(this, ConfirmBooking::class.java)
        intent.putExtra("selectedDate", formattedDate)
        intent.putExtra("selectedTime", formattedTime)
        intent.putExtra("doctorEmail", doctorEmail)
        intent.putExtra("patientEmail", patientEmail)
        intent.putExtra("bookingId", bookingId)
        intent.putExtra("timestampMillis", selectedDate)
        startActivity(intent)
    }

    private fun checkExistingBooking(patientEmail: String) {
        // Query ALL bookings for this patient, regardless of doctor
        bookingsRef.orderByChild("patientEmail").equalTo(patientEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasActiveBooking = false
                    var bookingStatus = ""

                    for (bookingSnapshot in snapshot.children) {
                        val status = bookingSnapshot.child("status").getValue(String::class.java)

                        // Check for "Pending" or "Confirmed" status bookings
                        if (status == "Pending" || status == "Confirmed") {
                            hasActiveBooking = true
                            bookingStatus = status
                            break
                        }
                    }

                    if (hasActiveBooking) {
                        AlertDialog.Builder(this@Booking)
                            .setTitle("Booking Already Exists")
                            .setMessage("You already have a $bookingStatus appointment. Please cancel it before booking a new one.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                finish() // Return to previous screen after dismissing the dialog
                            }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false) // Prevent dismissing by tapping outside
                            .show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@Booking,
                        "Error checking bookings: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun resetTimeSlots() {
        // Reset all time slots to default state
        val timeButtons = listOf(
            binding.btn910, binding.btn1011, binding.btn1112,
            binding.btn12, binding.btn23, binding.btn34, binding.btn45
        )

        for (btn in timeButtons) {
            btn.isEnabled = true
            btn.setBackgroundColor(Color.parseColor("#7A7A7A"))
            btn.setTextColor(Color.WHITE)
        }

        selectedTimeSlot = null
        selectedTimeText = ""
    }

    private fun fetchBookedTimeSlots(selectedDateMillis: Long) {
        // Convert milliseconds to date string format that matches your database
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val selectedDateStr = dateFormat.format(Date(selectedDateMillis))

        // Check if it's today, then disable past time slots
        if (isToday(selectedDateMillis)) {
            disablePastTimeSlots()
        }

        // First, check all bookings for this date regardless of doctor
        // to disable slots that are already booked with ANY doctor
        bookingsRef.orderByChild("date").equalTo(selectedDateStr)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (bookingSnapshot in snapshot.children) {
                        val timeFromDB = bookingSnapshot.child("time").getValue(String::class.java)
                        val status = bookingSnapshot.child("status").getValue(String::class.java)

                        // If there's a pending or confirmed booking in this time slot, disable it
                        if (status == "Pending" || status == "Confirmed") {
                            disableTimeSlot(timeFromDB)
                        }
                    }

                    // After checking all bookings, fetch doctor-specific bookings
                    fetchDoctorBookings(selectedDateStr)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@Booking,
                        "Error fetching bookings: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun fetchDoctorBookings(selectedDateStr: String) {
        // Query Firebase for booked slots on this date for this doctor
        bookingsRef.orderByChild("doctorEmail").equalTo(doctorEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (bookingSnapshot in snapshot.children) {
                        val dateFromDB = bookingSnapshot.child("date").getValue(String::class.java)
                        val timeFromDB = bookingSnapshot.child("time").getValue(String::class.java)
                        val status = bookingSnapshot.child("status").getValue(String::class.java)

                        // If this booking is for the selected date and status is Pending or Confirmed
                        if (dateFromDB == selectedDateStr && (status == "Pending" || status == "Confirmed")) {
                            // Disable the corresponding time slot
                            disableTimeSlot(timeFromDB)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@Booking,
                        "Error fetching doctor bookings: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
    private fun disableTimeSlot(bookedTime: String?) {
        if (bookedTime == null) return

        // Map the formatted time back to button
        val buttonTime = when {
            bookedTime.contains("9:00am to 10:00am") -> binding.btn910
            bookedTime.contains("10:00am to 11:00am") -> binding.btn1011
            bookedTime.contains("11:00am to 12:00pm") -> binding.btn1112
            bookedTime.contains("1:00pm to 2:00pm") -> binding.btn12
            bookedTime.contains("2:00pm to 3:00pm") -> binding.btn23
            bookedTime.contains("3:00pm to 4:00pm") -> binding.btn34
            bookedTime.contains("4:00pm to 5:00pm") -> binding.btn45
            else -> null
        }

        buttonTime?.let {
            it.isEnabled = false
            it.setBackgroundColor(Color.LTGRAY)
            it.setTextColor(Color.DKGRAY)
            // If this was the selected time slot, deselect it
            if (selectedTimeSlot == it) {
                selectedTimeSlot = null
                selectedTimeText = ""
            }
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance()
        selectedCal.timeInMillis = timestamp

        return (today.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR))
    }

    private fun disablePastTimeSlots() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)

        val timeButtons = listOf(
            Pair(binding.btn910, 9),
            Pair(binding.btn1011, 10),
            Pair(binding.btn1112, 11),
            Pair(binding.btn12, 13),
            Pair(binding.btn23, 14),
            Pair(binding.btn34, 15),
            Pair(binding.btn45, 16)
        )

        for ((btn, hour) in timeButtons) {
            if (currentHour > hour || (currentHour == hour && currentMinute >= 15)) {
                btn.isEnabled = false
                btn.setBackgroundColor(Color.LTGRAY)
                btn.setTextColor(Color.DKGRAY)
            }
        }
    }

    private fun formatTimeSlot(rawSlot: String): String {
        return when (rawSlot) {
            "9-10 AM" -> "9:00am to 10:00am"
            "10-11 AM" -> "10:00am to 11:00am"
            "11-12 PM" -> "11:00am to 12:00pm"
            "1-2 PM" -> "1:00pm to 2:00pm"
            "2-3 PM" -> "2:00pm to 3:00pm"
            "3-4 PM" -> "3:00pm to 4:00pm"
            "4-5 PM" -> "4:00pm to 5:00pm"
            else -> rawSlot  // fallback if already formatted
        }
    }
}