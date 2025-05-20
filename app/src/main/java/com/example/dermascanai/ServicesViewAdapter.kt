// ServicesViewAdapter.kt
package com.example.dermascanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServicesViewAdapter(
    private val servicesList: List<String>
) : RecyclerView.Adapter<ServicesViewAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceText: TextView = itemView.findViewById(R.id.serviceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_view, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.serviceText.text = "• ${servicesList[position]}"
    }

    override fun getItemCount(): Int = servicesList.size
}
