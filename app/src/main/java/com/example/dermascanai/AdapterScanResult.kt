package com.example.dermascanai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemScanresultBinding

class AdapterScanResult(
    private val scanList: List<Pair<String, ScanResult>>,
    private val onItemClick: (scanId: String, scan: ScanResult) -> Unit
) : RecyclerView.Adapter<AdapterScanResult.ScanResultViewHolder>() {

    inner class ScanResultViewHolder(val binding: ItemScanresultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultViewHolder {
        val binding = ItemScanresultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScanResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanResultViewHolder, position: Int) {
        val (scanId, scanResult) = scanList[position]
        holder.binding.textViewScan.text = scanResult.timestamp

        holder.itemView.setOnClickListener {
            onItemClick(scanId, scanResult)
        }
    }

    override fun getItemCount(): Int = scanList.size
}
