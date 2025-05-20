
// DermatologistsViewAdapter.kt
package com.example.dermascanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DermatologistsViewAdapter(
    private val dermatologistsList: MutableList<Dermatologist>
) : RecyclerView.Adapter<DermatologistsViewAdapter.DermatologistViewHolder>() {

    inner class DermatologistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.dermatologistName)
        val specializationText: TextView = itemView.findViewById(R.id.dermatologistSpecialization)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DermatologistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dermatologist_view, parent, false)
        return DermatologistViewHolder(view)
    }

    override fun onBindViewHolder(holder: DermatologistViewHolder, position: Int) {
        val dermatologist = dermatologistsList[position]
        holder.nameText.text = dermatologist.name
        holder.specializationText.text = dermatologist.specialization
    }

    override fun getItemCount(): Int = dermatologistsList.size
}