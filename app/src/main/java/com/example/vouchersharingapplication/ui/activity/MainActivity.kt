package com.example.vouchersharingapplication.ui.activity

import com.example.vouchersharingapplication.data.model.Urun
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vouchersharingapplication.data.model.ScrapeWorker
import com.example.vouchersharingapplication.data.network.generateRandomDocumentId
import com.example.vouchersharingapplication.data.network.scrapeCarrefourData
import com.example.vouchersharingapplication.databinding.ActivityMainBinding
import com.example.vouchersharingapplication.ui.adapter.UrunAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val urunList = mutableListOf<Urun>() // Kullanıcının eklediği ürünler
    private lateinit var urunAdapter: UrunAdapter
    private val db = FirebaseFirestore.getInstance()
    private val REQUEST_IMAGE_CAPTURE = 1
    private var selectedImageUrl: String? = null // Seçilen ürün resmini saklamak için
    private val urunMap = mutableMapOf<String, String>() // Ürün adı -> Resim URL'si
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // RecyclerView setup
        urunAdapter = UrunAdapter(urunList)
        binding.urunlerRecyclerView.adapter = urunAdapter
        binding.urunlerRecyclerView.layoutManager = LinearLayoutManager(this)

        // Fotoğraf çekme işlemi
        binding.fotografCekButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }

        // AutoCompleteTextView'den öğe seçildiğinde yapılacak işlem
        binding.urunAutoComplete.setOnItemClickListener { parent, view, position, id ->
            // Seçilen öğe
            val selectedUrunAdi = parent.getItemAtPosition(position) as String

            // Seçilen ürün adı ile resim URL'sini bulma
            val imageUrl = urunMap[selectedUrunAdi]

            if (imageUrl != null) {
                // Resim URL'si bulundu, selectedImageUrl'yi güncelle
                selectedImageUrl = imageUrl
                Log.d("AutoComplete", "Seçilen ürün: $selectedUrunAdi, Resim URL: $selectedImageUrl")
            } else {
                // Resim URL'si bulunamadı
                Log.e("AutoComplete", "Seçilen ürünün resim URL'si bulunamadı!")
            }
        }


        // Ürün ekleme butonu
        binding.urunEkleButton.setOnClickListener {
            val urunAdi = binding.urunAutoComplete.text.toString()
            val fiyatText = binding.fiyatEditText.text.toString()

            Log.d("UrunEkle", "Ürün adı: $urunAdi, Fiyat: $fiyatText, Resim URL: $selectedImageUrl")

            // Alanların doldurulup doldurulmadığını kontrol et
            if (urunAdi.isEmpty()) {
                Log.e("UrunEkle", "Hata: Ürün adı boş!")
                Toast.makeText(this, "Lütfen ürün adını girin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fiyatText.isEmpty()) {
                Log.e("UrunEkle", "Hata: Fiyat boş!")
                Toast.makeText(this, "Lütfen fiyat girin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUrl.isNullOrEmpty()) {
                Log.e("UrunEkle", "Hata: Resim URL boş!")
                Toast.makeText(this, "Lütfen bir ürün seçin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fiyat = fiyatText.toDoubleOrNull()
            if (fiyat == null) {
                Log.e("UrunEkle", "Hata: Fiyat geçerli değil! Girilen fiyat: $fiyatText")
                Toast.makeText(this, "Lütfen geçerli bir fiyat girin!", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("UrunEkle", "Ürün RecyclerView'a eklenecek: $urunAdi, Fiyat: $fiyat, Resim URL: $selectedImageUrl")
                addUrunToRecyclerView(urunAdi, fiyat)
            }
        }


        binding.fotografCekButton.setOnClickListener {
            // Kamera iznini kontrol et ve gerekirse iste
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
            } else {
                // İzin zaten verilmiş, işlemi başlat
                startCamera()
            }
        }

        binding.urunAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Eğer bir şey yapmanız gerekiyorsa burada yapabilirsiniz
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Kullanıcı yazdıkça çalışacak olan kod
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val query = charSequence.toString()
                    if (query.length >= 3) { // En az 3 karakter kontrolü
                        fetchAutocompleteData(query)
                    }
                }
                handler.postDelayed(searchRunnable!!, 1000) // 1000ms bekleme süresi
            }

            override fun afterTextChanged(editable: Editable?) {
                // Eğer bir şey yapmanız gerekiyorsa burada yapabilirsiniz
            }
        })

        // Periodik scraping işlemi başlatma
        startPeriodicScraping()
    }

    // Fotoğraf çekildikten sonra OCR işlemi yapılacak
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            recognizeTextFromImage(imageBitmap)
        }
    }

    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                processText(visionText.text) // OCR'dan dönen metni işle
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "OCR Hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processText(recognizedText: String) {
        val lines = recognizedText.split("\n")
        for (line in lines) {
            val result = extractUrunVeFiyat(line)
            result?.let {
                addUrunToRecyclerView(it.urunAdi, it.fiyat)
            }
        }
    }

    private fun extractUrunVeFiyat(line: String): Urun? {
        val fiyatRegex = Regex("(\\d{1,3}(\\.\\d{3})*,\\d{2})\\s*TL")
        val match = fiyatRegex.find(line)

        return if (match != null) {
            val fiyatText = match.groupValues[1]
            val fiyat = fiyatText.replace(".", "").replace(",", ".").toDouble()
            val urunAdi = line.substring(0, match.range.first).trim()
            Urun(urunAdi, fiyat)
        } else {
            null
        }
    }

    private fun addUrunToRecyclerView(urunAdi: String, fiyat: Double) {
        val selectedUrunAdi = binding.urunAutoComplete.text.toString()
        val imageUrl = urunMap[selectedUrunAdi]
        if (imageUrl == null) {
            Toast.makeText(this, "Ürünün resmi bulunamadı!", Toast.LENGTH_SHORT).show()
            return
        }
        urunList.add(Urun(selectedUrunAdi, fiyat, imageUrl))
        urunAdapter.notifyItemInserted(urunList.size - 1)
        // Alanları temizle
        binding.fiyatEditText.text.clear()
        binding.urunAutoComplete.text.clear()
        selectedImageUrl = null // Resmi sıfırla
        Toast.makeText(this, "Ürün RecyclerView'a eklendi", Toast.LENGTH_SHORT).show()
    }

    private fun fetchAutocompleteData(query: String) {
        val db = FirebaseFirestore.getInstance()
        val urunlerRef = db.collection("urunler")

        Log.d("Autocomplete", "Sorgu başlatıldı: $query")

        // Büyük küçük harf duyarlılığı olmadan karşılaştırma yap
        val queryLower = query.lowercase(Locale.ROOT)
        val queryParts = queryLower.split(" ")

        // Firestore'dan veri çekme
        urunlerRef
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("Autocomplete", "Sorgu başarılı, ${querySnapshot.size()} belge bulundu.")
                val autocompleteList = mutableSetOf<String>()
                urunMap.clear() // urunMap'i temizle

                // Belgeleri işle
                for (document in querySnapshot.documents) {
                    val urunAdi = document.getString("urunAdi")?.lowercase(Locale.ROOT)
                    val imageUrl = document.getString("imageUrl")

                    if (!urunAdi.isNullOrEmpty() && !imageUrl.isNullOrEmpty()) {
                        val matches = queryParts.all { part -> urunAdi.contains(part) }
                        if (matches && autocompleteList.add(urunAdi)) { // Sadece benzersizse ekle
                            urunMap[urunAdi] = imageUrl // urunMap'e ekle
                            Log.d("Autocomplete", "Eklendi: $urunAdi, Image URL: $imageUrl")
                        }
                    } else {
                        Log.w("Autocomplete", "Boş veri tespit edildi. Belge ID: ${document.id}")
                    }
                }

                // AutoCompleteTextView'e veri bağlama
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, autocompleteList.toList())
                binding.urunAutoComplete.setAdapter(adapter)
                adapter.notifyDataSetChanged()  // AutoCompleteTextView'e veri ekledikten sonra güncelle

                Log.d("Autocomplete", "Veri başarıyla bağlandı.")
            }
            .addOnFailureListener { e ->
                Log.e("Autocomplete", "Veri çekme hatası: ${e.message}")
            }
    }

    private fun startPeriodicScraping() {
        val workRequest = PeriodicWorkRequestBuilder<ScrapeWorker>(7, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "scraping_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
