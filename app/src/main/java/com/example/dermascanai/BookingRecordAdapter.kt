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
    private val onCancel: (BookingData) -> Unit // Parameter for cancellation
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
            val date = Date(booking.timestampMillis)

            // Set basic information
            binding.patientNameTv.text = booking.patientEmail
            binding.appointmentDateTv.text = dateFormat.format(date)
            binding.appointmentTimeTv.text = booking.time
            binding.notesTextView.text = booking.message

            // Configure status elements and color based on status
            configureStatusElements(booking)

            // Handle decline reason visibility
            if (!booking.declineReason.isNullOrEmpty() && booking.status == "declined") {
                binding.declineReasonLayout.visibility = View.VISIBLE
                binding.declineReasonTv.text = booking.declineReason
            } else {
                binding.declineReasonLayout.visibility = View.GONE
            }

            // Handle cancellation reason visibility - NEW CODE
            if (!booking.cancellationReason.isNullOrEmpty() && booking.status == "cancelled") {
                binding.cancellationReasonLayout.visibility = View.VISIBLE
                binding.cancellationReasonTv.text = booking.cancellationReason
            } else {
                binding.cancellationReasonLayout.visibility = View.GONE
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

        private fun configureStatusElements(booking: BookingData) {
            // Set action buttons visibility based on status
            when (booking.status) {
                "pending" -> {
                    binding.approveButton.visibility = View.VISIBLE
                    binding.declineButton.visibility = View.VISIBLE
                    binding.cancelButton.visibility = View.GONE
                    binding.statusLayout.visibility = View.GONE
                }
                "confirmed" -> { // Show cancel button for confirmed appointments - NEW CODE
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.statusLayout.visibility = View.VISIBLE

                    // Configure status bar with confirmed styling
                    binding.statusTextView.text = "Confirmed"
                    binding.statusLayout.setBackgroundResource(R.drawable.status_confirmed_background)
                    binding.statusIcon.setImageResource(R.drawable.check_circle)
                }
                "ongoing" -> {
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.statusLayout.visibility = View.VISIBLE

                    // Configure status bar with ongoing styling
                    binding.statusTextView.text = "Ongoing"
                    binding.statusLayout.setBackgroundResource(R.drawable.status_ongoing_background)
                    binding.statusIcon.setImageResource(R.drawable.ongoing)
                }
                "cancelled" -> { // Handle cancelled status - NEW CODE
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.cancelButton.visibility = View.GONE
                    binding.statusLayout.visibility = View.VISIBLE

                    // Configure status bar with cancelled styling
                    binding.statusTextView.text = "Cancelled"
                    binding.statusLayout.setBackgroundResource(R.drawable.status_cancelled_background)
                    binding.statusIcon.setImageResource(R.drawable.cancelled)
                }
                "declined" -> {
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.cancelButton.visibility = View.GONE
                    binding.statusLayout.visibility = View.VISIBLE

                    // Configure status bar with declined styling
                    binding.statusTextView.text = "Declined"
                    binding.statusLayout.setBackgroundResource(R.drawable.status_declined_background)
                    binding.statusIcon.setImageResource(R.drawable.close_circle)
                }
                else -> {
                    binding.approveButton.visibility = View.GONE
                    binding.declineButton.visibility = View.GONE
                    binding.cancelButton.visibility = View.GONE
                    binding.statusLayout.visibility = View.VISIBLE
                    binding.statusTextView.text = booking.status.capitalize(Locale.ROOT)
                }
            }
        }
    }
}