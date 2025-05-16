package com.example.dermascanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemBookingApprovalBinding
import java.text.SimpleDateFormat
import java.util.*

class BookingApprovalAdapter(
    private val bookings: List<BookingData>,
    private val onApprove: (BookingData) -> Unit,
    private val onDecline: (BookingData) -> Unit,
    private val onCancel: (BookingData) -> Unit // New parameter for cancellation
) : RecyclerView.Adapter<BookingApprovalAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingApprovalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(private val binding: ItemBookingApprovalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingData) {
            // Format the date
            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = Date(booking.timestampMillis)

            // Set basic information
            binding.patientNameTv.text = booking.patientEmail
            binding.appointmentDateTv.text = dateFormat.format(date)
            binding.appointmentTimeTv.text = timeFormat.format(date)
            binding.notesTextView.text = booking.message

            // Set status information
            binding.statusTextView.text = booking.status.capitalize()

            // Set action buttons visibility based on status
            when (booking.status) {
                "pending" -> {
                    binding.approveButton.visibility = View.VISIBLE
                    binding.declineButton.visibility = View.VISIBLE
                    binding.cancelButton.visibility = View.GONE
                    binding.statusLayout.visibility = View.GONE
                }
                "ongoing" -> {
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.statusLayout.visibility = View.VISIBLE
                }
                else -> {
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.cancelButton.visibility = View.GONE
                    binding.statusLayout.visibility = View.VISIBLE
                }
            }


            if (!booking.declineReason.isNullOrEmpty() && booking.status == "declined") {
                binding.declineReasonLayout.visibility = View.VISIBLE
                binding.declineReasonTv.text = booking.declineReason
            } else {
                binding.declineReasonLayout.visibility = View.GONE
            }

            // Set click listeners
            binding.approveButton.setOnClickListener {
                onApprove(booking)
            }

            binding.declineButton.setOnClickListener {
                onDecline(booking)
            }

            binding.cancelButton.setOnClickListener {
                onCancel(booking)
            }
        }
    }
}