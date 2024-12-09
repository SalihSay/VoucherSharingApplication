package com.example.vouchersharingapplication.data.model

import androidx.recyclerview.widget.DiffUtil

class UrunDiffCallback(
    private val oldList: List<Urun>,
    private val newList: List<Urun>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Bu kısımda ürünlerin benzersiz kimliğine göre karşılaştırma yapıyoruz
        return oldList[oldItemPosition].urunAdi == newList[newItemPosition].urunAdi
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Burada ise ürünlerin içeriğini karşılaştırıyoruz, örneğin fiyatları ve isimleri
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
