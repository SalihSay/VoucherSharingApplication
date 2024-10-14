package com.example.vouchersharingapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.vouchersharingapplication.data.model.Urun
import com.example.vouchersharingapplication.databinding.UrunItemBinding
import com.squareup.picasso.Picasso


class UrunAdapter(private val urunList: List<Urun>) : RecyclerView.Adapter<UrunAdapter.UrunViewHolder>() {

    class UrunViewHolder(val binding: UrunItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrunViewHolder {
        val binding = UrunItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UrunViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UrunViewHolder, position: Int) {
        val currentUrun = urunList[position]
        holder.binding.urunAdiTextView.text = currentUrun.urunAdi
        holder.binding.fiyatTextView.text = currentUrun.fiyat.toString()

        // Ürün resmini yükleyin
        Picasso.get()
            .load(currentUrun.imageUrl)
            .into(holder.binding.urunImageView) // ImageView'iniz
    }

    override fun getItemCount() = urunList.size
}
