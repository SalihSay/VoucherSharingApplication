package com.example.vouchersharingapplication.ui.activity

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vouchersharingapplication.data.model.Urun
import com.example.vouchersharingapplication.databinding.ActivityMainBinding
import com.example.vouchersharingapplication.ui.adapter.UrunAdapter
import com.example.vouchersharingapplication.data.network.scrapeCarrefourData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vouchersharingapplication.data.model.ScrapeWorker
import com.example.vouchersharingapplication.data.model.SpaceItemDecoration
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val urunList = mutableListOf<Urun>() // Seçilen ürünleri tutacak liste
    private val urunAdiList = mutableListOf<String>() // Ürün isimlerini tutacak liste
    private lateinit var urunAdapter: UrunAdapter
    private lateinit var urunAdapterSpinner: ArrayAdapter<String>
    private val db = FirebaseFirestore.getInstance()
    private var scrapedUrunler = mutableListOf<Urun>() // Scraped ürünleri tutacak liste

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // RecyclerView setup
        urunAdapter = UrunAdapter(urunList)
        binding.urunlerRecyclerView.adapter = urunAdapter
        binding.urunlerRecyclerView.layoutManager = LinearLayoutManager(this)


        binding.urunlerRecyclerView.addItemDecoration(SpaceItemDecoration(4))

        // Spinner için ArrayAdapter oluşturma
        urunAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, urunAdiList)
        urunAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.urunAdiSpinner.adapter = urunAdapterSpinner // Spinner'a adapter ekle

        setupWorkManager()
        // Ürün verilerini çekme
        fetchUrunlerFromFirebase()


        // Ürün Ekle Butonuna tıklama
        binding.urunEkleButton.setOnClickListener {
            val urunAdi = binding.urunAdiSpinner.selectedItem.toString() // Spinner'dan seçilen ürünü al
            val fiyatText = binding.fiyatEditText.text.toString()

            // Eğer boşsa, kullanıcıya uyarı ver
            if (urunAdi.isEmpty() || fiyatText.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
            } else {
                val fiyat = fiyatText.toDoubleOrNull()
                if (fiyat == null) {
                    Toast.makeText(this, "Lütfen geçerli bir fiyat girin!", Toast.LENGTH_SHORT).show()
                } else {
                    // Seçilen ürünü bul
                    val selectedUrun = scrapedUrunler.find { it.urunAdi == urunAdi }

                    if (selectedUrun != null) {
                        // Kullanıcının girdiği fiyatı ekle
                        val yeniUrun = Urun(selectedUrun.urunAdi, fiyat, selectedUrun.imageUrl)

                        // Firestore'a ekleme
                        db.collection("urunler")
                            .add(yeniUrun)
                            .addOnSuccessListener {
                                urunList.add(yeniUrun)
                                urunAdiList.add(yeniUrun.urunAdi)
                                urunAdapter.notifyItemInserted(urunList.size - 1)
                                Toast.makeText(this, "Ürün başarıyla eklendi", Toast.LENGTH_SHORT).show()

                                // Text alanlarını temizle
                                binding.fiyatEditText.text.clear()
                                binding.urunAdiSpinner.setSelection(0)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Ürün eklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Seçilen ürün bulunamadı!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    private fun setupWorkManager() {
        val scrapeWorkRequest = PeriodicWorkRequestBuilder<ScrapeWorker>(12, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ScrapingWork",
            ExistingPeriodicWorkPolicy.KEEP,
            scrapeWorkRequest
        )
    }


    // Firestore'dan verileri çekme fonksiyonu
    private fun fetchUrunlerFromFirebase() {
        db.collection("urunler") // "urunler" koleksiyonunu seçin
            .get()
            .addOnSuccessListener { documents ->
                scrapedUrunler.clear() // Mevcut ürünleri temizle
                urunAdiList.clear() // Spinner için ürün adlarını temizle

                for (document in documents) {
                    val urun = document.toObject(Urun::class.java) // Dokümanı Urun nesnesine dönüştür
                    scrapedUrunler.add(urun) // Scraped ürünleri listeye ekle
                    urunAdiList.add(urun.urunAdi) // Ürün adını spinner için listeye ekle
                }

                // Spinner için adapteri güncelle
                urunAdapterSpinner.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Veri çekme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    // Scraping işlemini gerçekleştiren fonksiyon
    private fun fetchScrapedUrunler() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scrapeCarrefourData { scrapedUrunlerList ->
                    // Scraped ürünleri Firestore'a ekle ve Firebase'den senkronize şekilde eklenen ürünleri çek
                    scrapedUrunlerList.forEach { urun ->
                        db.collection("urunler").add(urun)
                    }

                    // Veriler Firestore'a eklendikten sonra Spinner'ı güncelle
                    db.collection("urunler")
                        .get()
                        .addOnSuccessListener { documents ->
                            runOnUiThread {
                                urunAdiList.clear()
                                urunAdiList.addAll(documents.map { it.toObject(Urun::class.java).urunAdi })
                                urunAdapterSpinner.notifyDataSetChanged() // Spinner güncelleme
                                Toast.makeText(this@MainActivity, "${scrapedUrunlerList.size} ürün başarıyla yüklendi.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Veri çekme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ürün verileri çekilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}
