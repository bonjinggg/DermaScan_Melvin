package com.example.dermascanai

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemDermaListBinding

class AdapterDermaHomeList(private val userList: List<DermaInfo>) :
    RecyclerView.Adapter<AdapterDermaHomeList.DermaUserViewHolder>() {

    inner class DermaUserViewHolder(val binding: ItemDermaListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DermaUserViewHolder {
        val binding = ItemDermaListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DermaUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DermaUserViewHolder, position: Int) {
        val user = userList[position]
        with(holder.binding) {
            name.text = user.name ?: "No Name"

            val profileImageString = user.profileImage
            if (!profileImageString.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(profileImageString, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profPic.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    profPic.setImageResource(R.drawable.ic_profile2)
                }
            } else {
                profPic.setImageResource(R.drawable.ic_profile2)
            }

            root.setOnClickListener {
                val context = it.context
                val intent = Intent(context, DermaDetails::class.java).apply {
                    putExtra("userEmail", user.email)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = userList.size
}
