package com.example.vouchersharingapplication.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vouchersharingapplication.data.model.Urun
import com.example.vouchersharingapplication.databinding.ActivityBolusturmeBinding
import com.example.vouchersharingapplication.ui.adapter.ArkadasAdapter

class BolusturmeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBolusturmeBinding
    private var selectedUrun: Urun? = null
    private val arkadasList = mutableListOf<String>()
    private lateinit var arkadasAdapter: ArkadasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBolusturmeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("BolusturmeActivity", "onCreate: Activity created")

        // Intent'ten seçilen ürünü al
        selectedUrun = intent.getParcelableExtra("selectedUrun")

        if (selectedUrun == null) {
            Log.e("BolusturmeActivity", "onCreate: Seçilen ürün bilgisi bulunamadı.")
            Toast.makeText(this, "Ürün bilgisi bulunamadı!", Toast.LENGTH_SHORT).show()
            finish() // Aktiviteyi sonlandır
            return
        }

        // Seçilen ürün bilgilerini göster
        binding.bolusturmeUrunAdi.text = selectedUrun!!.urunAdi
        binding.bolusturmeUrunFiyat.text = "${selectedUrun!!.fiyat} TL"
        Log.d("BolusturmeActivity", "onCreate: Displaying selected product: ${selectedUrun!!.urunAdi}, Price: ${selectedUrun!!.fiyat}")

        // "Siz" kullanıcısını varsayılan olarak ekle
        arkadasList.add("Siz")
        Log.d("BolusturmeActivity", "onCreate: Added 'Siz' to arkadasList")

        // Arkadaş listesi için RecyclerView
        arkadasAdapter = ArkadasAdapter(arkadasList)
        binding.arkadasRecyclerView.adapter = arkadasAdapter
        binding.arkadasRecyclerView.layoutManager = LinearLayoutManager(this)
        Log.d("BolusturmeActivity", "onCreate: RecyclerView for arkadasList setup complete")

        // Arkadaş ekleme butonu
        binding.arkadasEkleButton.setOnClickListener {
            Log.d("BolusturmeActivity", "arkadasEkleButton: onClick triggered")
            val arkadasAdi = binding.arkadasAdiEditText.text.toString().trim()
            if (arkadasAdi.isNotEmpty()) {
                // "Siz" zaten listede olduğu için, tekrar eklemeyi engelle
                if (arkadasAdi != "Siz") {
                    arkadasList.add(arkadasAdi)
                    arkadasAdapter.notifyItemInserted(arkadasList.size - 1)
                    binding.arkadasAdiEditText.text.clear()
                    Log.d("BolusturmeActivity", "arkadasEkleButton: Added new friend: $arkadasAdi")
                } else {
                    Log.d("BolusturmeActivity", "arkadasEkleButton: 'Siz' is already in the list")
                    Toast.makeText(this, "\"Siz\" kullanıcısı zaten listede!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("BolusturmeActivity", "arkadasEkleButton: Friend name is empty")
                Toast.makeText(this, "Lütfen arkadaşınızın adını girin!", Toast.LENGTH_SHORT).show()
            }
        }

        // Bölüştür butonu
        binding.bolusturButton.setOnClickListener {
            Log.d("BolusturmeActivity", "bolusturButton: onClick triggered")
            val ortakSayisi = arkadasAdapter.ortakOlanlar.values.count { it } // Kaç kişinin seçili olduğunu say

            if (ortakSayisi > 0) {
                val kisiBasiFiyat = selectedUrun!!.fiyat / ortakSayisi
                val sonucMesaji = buildString {
                    append("Kişi başı fiyat: ${String.format("%.2f", kisiBasiFiyat)} TL\n")
                    append("Ortak olanlar:\n")
                    // "Siz" kullanıcısı da dahil, seçili kişileri ekle
                    arkadasList.filterIndexed { index, _ -> arkadasAdapter.ortakOlanlar[index] == true }
                        .forEach { append("$it\n") }
                }
                binding.bolusturmeSonucTextView.text = sonucMesaji
                Log.d("BolusturmeActivity", "bolusturButton: Bolusturme complete, result: $sonucMesaji")
            } else {
                Log.e("BolusturmeActivity", "bolusturButton: No participants selected")
                Toast.makeText(this, "Lütfen en az bir kişi seçin!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}