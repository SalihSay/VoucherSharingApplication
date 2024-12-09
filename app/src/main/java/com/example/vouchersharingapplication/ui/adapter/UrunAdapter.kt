package com.example.vouchersharingapplication.ui.adapter

import android.util.Log
import com.example.vouchersharingapplication.data.model.Urun
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.vouchersharingapplication.R
import com.example.vouchersharingapplication.data.model.UrunDiffCallback
import com.example.vouchersharingapplication.databinding.UrunItemBinding
import com.squareup.picasso.Picasso

class UrunAdapter(private val urunList: MutableList<Urun>) : RecyclerView.Adapter<UrunAdapter.UrunViewHolder>() {

    class UrunViewHolder(val binding: UrunItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrunViewHolder {
        val binding = UrunItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UrunViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UrunViewHolder, position: Int) {
        val currentUrun = urunList[position]

        // Log: Ürün bilgilerini yazdırıyoruz
        Log.d("UrunAdapter", "Ürün Adı: ${currentUrun.urunAdi}, Fiyat: ${currentUrun.fiyat}, Image URL: ${currentUrun.imageUrl}")

        holder.binding.urunAdiTextView.text = currentUrun.urunAdi
        holder.binding.fiyatTextView.text = currentUrun.fiyat.toString()

        // Ürün resmini yükleyin
        Picasso.get()
            .load(currentUrun.imageUrl)
            .into(holder.binding.urunImageView) // ImageView'iniz
    }

    override fun getItemCount() = urunList.size

    // Listeyi güncellemek için DiffUtil kullanımı
    fun updateList(newList: List<Urun>) {
        val diffCallback = UrunDiffCallback(urunList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // Log: Güncellenen listeyi yazdırıyoruz
        Log.d("UrunAdapter", "Liste güncelleniyor. Yeni liste: $newList")

        urunList.clear()
        urunList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    // Öğeyi eklemek için
    fun addItem(urun: Urun) {
        // Log: Eklenen ürünün bilgilerini yazdırıyoruz
        Log.d("UrunAdapter", "Yeni ürün eklendi: Ad: ${urun.urunAdi}, Fiyat: ${urun.fiyat}, Image URL: ${urun.imageUrl}")

        urunList.add(urun)
        notifyItemInserted(urunList.size - 1)
    }
}
