package com.example.vouchersharingapplication.ui.adapter

import android.util.Log
import com.example.vouchersharingapplication.data.model.Urun
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.vouchersharingapplication.R
import com.example.vouchersharingapplication.data.model.UrunDiffCallback
import com.example.vouchersharingapplication.databinding.UrunItemBinding

class UrunAdapter(private val urunList: MutableList<Urun>) : RecyclerView.Adapter<UrunAdapter.UrunViewHolder>() {

    class UrunViewHolder(val binding: UrunItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrunViewHolder {
        val binding = UrunItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UrunViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UrunViewHolder, position: Int) {
        val currentUrun = urunList[position]

        Log.d("UrunAdapter", "onBindViewHolder: Binding product: ${currentUrun.urunAdi}, Price: ${currentUrun.fiyat}, Image URL: ${currentUrun.imageUrl}")

        holder.binding.urunAdiTextView.text = currentUrun.urunAdi
        holder.binding.fiyatTextView.text = String.format("%.2f", currentUrun.fiyat) + " TL"

        // Ürün resmini yükleyin
        if (currentUrun.imageUrl.isNotEmpty()) {
            holder.binding.urunImageView.load(currentUrun.imageUrl) {
                placeholder(R.drawable.placeholder_image)
                error(R.drawable.error_image)

            }
        } else {
            Log.d("UrunAdapter", "onBindViewHolder: Image URL is empty for product: ${currentUrun.urunAdi}")
            holder.binding.urunImageView.visibility = View.GONE
        }
    }

    override fun getItemCount() = urunList.size

    // Listeyi güncellemek için DiffUtil kullanımı
    fun updateList(newList: List<Urun>) {
        Log.d("UrunAdapter", "updateList: Updating list with new size: ${newList.size}")
        val diffCallback = UrunDiffCallback(urunList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        urunList.clear()
        urunList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    // Öğeyi eklemek için
    fun addItem(urun: Urun) {
        Log.d("UrunAdapter", "addItem: Adding product: ${urun.urunAdi}, Price: ${urun.fiyat}, Image URL: ${urun.imageUrl}")
        urunList.add(urun)
        notifyItemInserted(urunList.size - 1)
    }
}