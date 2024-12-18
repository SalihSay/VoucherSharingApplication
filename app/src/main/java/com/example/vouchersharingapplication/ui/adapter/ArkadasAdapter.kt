package com.example.vouchersharingapplication.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.vouchersharingapplication.databinding.ArkadasItemBinding

class ArkadasAdapter(private val arkadasList: List<String>) : RecyclerView.Adapter<ArkadasAdapter.ArkadasViewHolder>() {

    val ortakOlanlar = mutableMapOf<Int, Boolean>().withDefault { false }

    class ArkadasViewHolder(val binding: ArkadasItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArkadasViewHolder {
        val binding = ArkadasItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        Log.d("ArkadasAdapter", "onCreateViewHolder: Creating view holder for ${binding}")
        return ArkadasViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArkadasViewHolder, position: Int) {
        val arkadasAdi = arkadasList[position]
        Log.d("ArkadasAdapter", "onBindViewHolder: Binding view holder for position: $position, Friend: $arkadasAdi")
        holder.binding.arkadasAdiTextView.text = arkadasAdi
        // CheckBox'ın başlangıç durumunu ve değişim dinleyicisini ayarla
        holder.binding.ortakCheckBox.isChecked = ortakOlanlar.getValue(position)
        holder.binding.ortakCheckBox.setOnCheckedChangeListener { _, isChecked ->
            Log.d("ArkadasAdapter", "onBindViewHolder: CheckBox state changed for position: $position, isChecked: $isChecked")
            ortakOlanlar[position] = isChecked
        }
    }

    override fun getItemCount(): Int {
        Log.d("ArkadasAdapter", "getItemCount: ${arkadasList.size}")
        return arkadasList.size
    }
}