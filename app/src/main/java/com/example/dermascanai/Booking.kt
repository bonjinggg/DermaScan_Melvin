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
            binding.btn12, binding.btn23, binding.btn34
        )

        for (btn in timeButtons) {
            btn.setOnClickListener {

                selectedTimeSlot?.setBackgroundColor(Color.parseColor("#7A7A7A"))

                btn.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                selectedTimeSlot = btn
                selectedTimeText = btn.text.toString()
            }
        }

//        binding.btnConfirm.setOnClickListener {
//            if (selectedDate == 0L || selectedTimeText.isEmpty()) {
//                Toast.makeText(this, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
//            } else {
//                val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))
//                Toast.makeText(this, "Booked on $date at $selectedTimeText", Toast.LENGTH_LONG).show()
//            }
//
//        }
    }
}