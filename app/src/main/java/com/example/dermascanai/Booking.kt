package com.example.dermascanai

import android.content.Intent
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dermascanai.databinding.ActivityBookingBinding
import java.util.Date
import java.util.Locale

class Booking : AppCompatActivity() {
    private lateinit var binding: ActivityBookingBinding
    private var selectedTimeSlot: Button? = null
    private var selectedDate: Long = 0L
    private var selectedTimeText: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val docEmail = intent.getStringExtra("doctorEmail")


        binding.calendarView.setWeekSeparatorLineColor(Color.BLACK)
        binding.calendarView.setFocusedMonthDateColor(Color.BLACK)
        binding.calendarView.setUnfocusedMonthDateColor(Color.BLACK)

        binding.backBTN.setOnClickListener {
            finish()
        }

        binding.btnConfirm.setOnClickListener {
            val intent = Intent(this, ConfirmBooking::class.java)
            startActivity(intent)
        }


        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
        }

        val timeButtons = listOf(
            binding.btn910, binding.btn1011, binding.btn1112,
            binding.btn12, binding.btn23, binding.btn34, binding.btn45
        )

        for (btn in timeButtons) {
            btn.setOnClickListener {

                selectedTimeSlot?.setBackgroundColor(Color.parseColor("#7A7A7A"))

                btn.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                selectedTimeSlot = btn
                selectedTimeText = btn.text.toString()
            }
        }

        binding.btnConfirm.setOnClickListener {
            if (selectedDate == 0L || selectedTimeText.isEmpty()) {
                Toast.makeText(this, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnConfirm.setOnClickListener {
                    if (selectedDate == 0L || selectedTimeText.isEmpty()) {
                        Toast.makeText(this, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
                    } else {
                        // Format the selected date
                        val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))

                        // Convert time slot if needed
                        val formattedTime = formatTimeSlot(selectedTimeText)

                        // Pass to next activity
                        val intent = Intent(this, ConfirmBooking::class.java)
                        intent.putExtra("selectedDate", formattedDate)
                        intent.putExtra("selectedTime", formattedTime)
                        intent.putExtra("doctorEmail", docEmail)
                        startActivity(intent)
                    }
                }

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