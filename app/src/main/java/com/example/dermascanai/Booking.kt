package com.example.dermascanai

import android.content.Intent
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dermascanai.databinding.ActivityBookingBinding
import com.google.firebase.database.*
import java.util.Date
import java.util.Locale

class Booking : AppCompatActivity() {
    private lateinit var binding: ActivityBookingBinding
    private var selectedService: Button? = null
    private var selectedDate: Long = 0L
    private var selectedServiceText: String = ""
    private var patientEmail: String = ""
    private var clinicName: String = ""
    private lateinit var database: FirebaseDatabase
    private lateinit var bookingsRef: DatabaseReference
    private val MAX_BOOKINGS_PER_DAY = 3

    private val serviceButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
        bookingsRef = database.getReference("bookings")

        // Get intent data
        patientEmail = intent.getStringExtra("patientEmail") ?: ""
        clinicName = intent.getStringExtra("clinicName") ?: ""

        println("Patient Email: $patientEmail")
        println("Clinic Name: $clinicName")

        // Initialize components
        setupToolbar()
        setupCalendar()
        fetchClinicServices()
        checkExistingBooking(patientEmail)

        // Set up listeners
        setupListeners()
    }

    private fun setupToolbar() {
        binding.backBTN.setOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        // Calendar date selection
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            handleDateSelection(year, month, dayOfMonth)
        }

        // Next button - Updated to use btnNext instead of btnConfirm
        binding.btnNext.setOnClickListener {
            if (selectedDate == 0L || selectedServiceText.isEmpty()) {
                Toast.makeText(this, "Please select a date and service", Toast.LENGTH_SHORT).show()
            } else {
                verifyNoExistingBookingsAndProceed()
            }
        }
    }

    private fun fetchClinicServices() {
        val clinicsRef = database.getReference("clinicInfo")

        // Clear existing services and show loading
        binding.servicesContainer.removeAllViews()
        showLoadingForServices()

        clinicsRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val clinicSnapshot = snapshot.children.firstOrNull()
                val clinicInfo = clinicSnapshot?.getValue(ClinicInfo::class.java)

                if (clinicInfo != null) {
                    displayClinicName(clinicInfo.name ?: "Unknown Clinic")

                    if (clinicName.isEmpty()) {
                        clinicName = clinicInfo.name ?: "Unknown Clinic"
                    }

                    val services = clinicInfo.services ?: listOf()
                    if (services.isNotEmpty()) {
                        setupDynamicServiceButtons(services)
                    } else {
                        showNoServicesMessage()
                    }
                } else {
                    showNoClinicDataMessage()
                }
            } else {
                showNoClinicDataMessage()
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error loading services: ${error.message}", Toast.LENGTH_SHORT).show()
            clearServicesContainer()
        }
    }


    private fun displayClinicName(clinicName: String) {
        // You can update the toolbar title or show a toast
        supportActionBar?.title = clinicName
    }

    private fun showLoadingForServices() {
        val loadingText = android.widget.TextView(this)
        loadingText.text = "Loading services..."
        loadingText.gravity = Gravity.CENTER
        loadingText.setPadding(16, 32, 16, 32)
        loadingText.setTextColor(Color.GRAY)
        loadingText.textSize = 16f
        binding.servicesContainer.addView(loadingText)
    }

    private fun showNoServicesMessage() {
        clearServicesContainer()
        val messageText = android.widget.TextView(this)
        messageText.text = "No services available for this clinic"
        messageText.gravity = Gravity.CENTER
        messageText.setPadding(16, 32, 16, 32)
        messageText.setTextColor(Color.RED)
        messageText.textSize = 16f
        binding.servicesContainer.addView(messageText)

        Toast.makeText(this, "No services found for this clinic", Toast.LENGTH_LONG).show()
    }

    private fun showNoClinicDataMessage() {
        clearServicesContainer()
        val messageText = android.widget.TextView(this)
        messageText.text = "No clinic information available"
        messageText.gravity = Gravity.CENTER
        messageText.setPadding(16, 32, 16, 32)
        messageText.setTextColor(Color.RED)
        messageText.textSize = 16f
        binding.servicesContainer.addView(messageText)

        Toast.makeText(this, "No clinic information available", Toast.LENGTH_SHORT).show()
    }

    private fun clearServicesContainer() {
        binding.servicesContainer.removeAllViews()
        serviceButtons.clear()
    }

    private fun setupDynamicServiceButtons(services: List<String>) {
        clearServicesContainer()

        // Create layout parameters for service buttons
        val buttonLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            140 // Fixed height for consistency
        )
        buttonLayoutParams.setMargins(0, 0, 0, 16) // Bottom margin between buttons

        for (service in services) {
            val button = Button(this)
            button.text = service
            button.layoutParams = buttonLayoutParams

            // Style the button to match your design
            button.setBackgroundColor(Color.parseColor("#7A7A7A"))
            button.setTextColor(Color.WHITE)
            button.gravity = Gravity.CENTER
            button.setPadding(16, 16, 16, 16)
            button.textSize = 16f


            button.background = resources.getDrawable(R.drawable.service_button_bg, null)

            button.setOnClickListener {
                if (button.isEnabled) {
                    // Reset previous selection
                    selectedService?.setBackgroundColor(Color.parseColor("#7A7A7A"))

                    // Set new selection
                    button.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                    selectedService = button
                    selectedServiceText = button.text.toString()

                    Toast.makeText(this, "Selected: $selectedServiceText", Toast.LENGTH_SHORT).show()
                }
            }

            binding.servicesContainer.addView(button)
            serviceButtons.add(button)
        }

        println("Successfully created ${serviceButtons.size} service buttons")
    }

    private fun setupCalendar() {
        // Style the calendar
        binding.calendarView.setWeekSeparatorLineColor(Color.BLACK)
        binding.calendarView.setFocusedMonthDateColor(Color.BLACK)
        binding.calendarView.setUnfocusedMonthDateColor(Color.BLACK)

        // Set minimum date to tomorrow
        val tomorrow = Calendar.getInstance()
        tomorrow.set(Calendar.HOUR_OF_DAY, 0)
        tomorrow.set(Calendar.MINUTE, 0)
        tomorrow.set(Calendar.SECOND, 0)
        tomorrow.set(Calendar.MILLISECOND, 0)
        tomorrow.add(Calendar.DAY_OF_MONTH, 1)

        binding.calendarView.minDate = tomorrow.timeInMillis
    }

    private fun handleDateSelection(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        selectedDate = calendar.timeInMillis

        // Reset service selection when date changes
        resetServiceSelection()

        // Check availability for the selected date
        checkAvailableBookingsForDate(selectedDate)

        // Format and show selected date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(selectedDate))
        Toast.makeText(this, "Selected date: $formattedDate", Toast.LENGTH_SHORT).show()
    }

    private fun verifyNoExistingBookingsAndProceed() {
        if (selectedDate == 0L || selectedServiceText.isEmpty()) {
            Toast.makeText(this, "Please select a date and service", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for existing bookings
        bookingsRef.orderByChild("patientEmail").equalTo(patientEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasActiveBooking = false
                    var bookingStatus = ""

                    for (bookingSnapshot in snapshot.children) {
                        val status = bookingSnapshot.child("status").getValue(String::class.java)
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
                            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    } else {
                        proceedWithBooking()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Booking, "Error checking bookings: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun proceedWithBooking() {
        val bookingId = "${System.currentTimeMillis()}"
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(selectedDate))

        val intent = Intent(this, ConfirmBooking::class.java)
        intent.putExtra("selectedDate", formattedDate)
        intent.putExtra("selectedService", selectedServiceText)
        intent.putExtra("patientEmail", patientEmail)
        intent.putExtra("clinicName", clinicName)
        intent.putExtra("bookingId", bookingId)
        intent.putExtra("timestampMillis", selectedDate)


        startActivity(intent)
    }

    private fun checkExistingBooking(patientEmail: String) {
        bookingsRef.orderByChild("patientEmail").equalTo(patientEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasActiveBooking = false
                    var bookingStatus = ""

                    for (bookingSnapshot in snapshot.children) {
                        val status = bookingSnapshot.child("status").getValue(String::class.java)
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
                                finish()
                            }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Booking, "Error checking bookings: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun resetServiceSelection() {
        for (btn in serviceButtons) {
            btn.isEnabled = true
            btn.setBackgroundColor(Color.parseColor("#7A7A7A"))
            btn.setTextColor(Color.WHITE)
        }

        selectedService = null
        selectedServiceText = ""
    }

    private fun checkAvailableBookingsForDate(selectedDateMillis: Long) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val selectedDateStr = dateFormat.format(Date(selectedDateMillis))

        bookingsRef.orderByChild("date").equalTo(selectedDateStr)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var bookingsCount = 0

                    for (bookingSnapshot in snapshot.children) {
                        val status = bookingSnapshot.child("status").getValue(String::class.java)
                        if (status == "Pending" || status == "Confirmed") {
                            bookingsCount++
                        }
                    }

                    val remainingBookings = MAX_BOOKINGS_PER_DAY - bookingsCount
                    binding.bookingsAvailableText.text = "Available bookings: $remainingBookings/$MAX_BOOKINGS_PER_DAY"

                    if (bookingsCount >= MAX_BOOKINGS_PER_DAY) {
                        disableAllServiceButtons()
                        AlertDialog.Builder(this@Booking)
                            .setTitle("No Availability")
                            .setMessage("All bookings for this date have been filled. Please select another date.")
                            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    } else {
                        // Re-enable service buttons if they were disabled
                        enableAllServiceButtons()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Booking, "Error checking bookings: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun disableAllServiceButtons() {
        for (btn in serviceButtons) {
            btn.isEnabled = false
            btn.setBackgroundColor(Color.LTGRAY)
            btn.setTextColor(Color.DKGRAY)
        }

        selectedService = null
        selectedServiceText = ""
    }

    private fun enableAllServiceButtons() {
        for (btn in serviceButtons) {
            btn.isEnabled = true
            if (btn != selectedService) {
                btn.setBackgroundColor(Color.parseColor("#7A7A7A"))
                btn.setTextColor(Color.WHITE)
            }
        }
    }
}