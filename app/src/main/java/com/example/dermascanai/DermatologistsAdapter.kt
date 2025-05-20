package com.example.dermascanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DermatologistsAdapter(
    private val dermatologistsList: MutableList<Dermatologist>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<DermatologistsAdapter.DermatologistViewHolder>() {

    class DermatologistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dermatologistName: TextView = itemView.findViewById(R.id.dermatologistName)
        val dermatologistSpecialization: TextView = itemView.findViewById(R.id.dermatologistSpecialization)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteDermatologistBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DermatologistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dermatologist, parent, false)
        return DermatologistViewHolder(view)
    }

    override fun onBindViewHolder(holder: DermatologistViewHolder, position: Int) {
        val dermatologist = dermatologistsList[position]
        holder.dermatologistName.text = dermatologist.name
        holder.dermatologistSpecialization.text = dermatologist.specialization

        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = dermatologistsList.size
}