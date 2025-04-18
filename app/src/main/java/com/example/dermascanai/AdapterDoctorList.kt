package com.example.dermascanai

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemDermaUserBinding

class AdapterDoctorList(private val userList: List<User>) :
    RecyclerView.Adapter<AdapterDoctorList.DermaUserViewHolder>() {

    inner class DermaUserViewHolder(val binding: ItemDermaUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DermaUserViewHolder {
        val binding = ItemDermaUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DermaUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DermaUserViewHolder, position: Int) {
        val user = userList[position]
        with(holder.binding) {
            textViewName.text = user.fullName


            if (!user.imageUrl.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(user.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageViewProfile.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageViewProfile.setImageResource(R.drawable.ic_profile2)
                }
            } else {
                imageViewProfile.setImageResource(R.drawable.ic_profile2)
            }
        }
    }

    override fun getItemCount(): Int = userList.size
}
