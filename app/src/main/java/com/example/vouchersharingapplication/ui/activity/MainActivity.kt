package com.example.vouchersharingapplication.ui.activity

import com.example.vouchersharingapplication.data.model.Urun
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vouchersharingapplication.data.model.ScrapeWorker
import com.example.vouchersharingapplication.databinding.ActivityMainBinding
import com.example.vouchersharingapplication.ui.adapter.UrunAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val urunList = mutableListOf<Urun>() // Kullanıcının eklediği ürünler
    private lateinit var urunAdapter: UrunAdapter
    private val db = FirebaseFirestore.getInstance()
    private val PICK_IMAGE_REQUEST = 101
    private val urunMap = mutableMapOf<String, String>() // Ürün adı -> Resim URL'si
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var selectedUrun: Urun? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "onCreate: Activity created")

        // RecyclerView setup
        urunAdapter = UrunAdapter(urunList)
        binding.urunlerRecyclerView.adapter = urunAdapter
        binding.urunlerRecyclerView.layoutManager = LinearLayoutManager(this)
        Log.d("MainActivity", "onCreate: RecyclerView setup complete")

        // AutoCompleteTextView'den öğe seçildiğinde yapılacak işlem
        binding.urunAutoComplete.setOnItemClickListener { parent, view, position, id ->
            Log.d("MainActivity", "urunAutoComplete: onItemClick triggered")
            val selectedUrunAdi = parent.getItemAtPosition(position) as String
            val imageUrl = urunMap[selectedUrunAdi]

            if (imageUrl != null) {
                Log.d("AutoComplete", "Seçilen ürün: $selectedUrunAdi, Resim URL: $imageUrl")
            } else {
                Log.e("AutoComplete", "Seçilen ürünün resim URL'si bulunamadı!")
            }
        }

        // Ürün ekleme butonu
        binding.urunEkleButton.setOnClickListener {
            Log.d("MainActivity", "urunEkleButton: onClick triggered")
            val urunAdi = binding.urunAutoComplete.text.toString()
            val fiyatText = binding.fiyatEditText.text.toString()

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

            val fiyat = fiyatText.toDoubleOrNull()
            if (fiyat == null) {
                Log.e("UrunEkle", "Hata: Fiyat geçerli değil! Girilen fiyat: $fiyatText")
                Toast.makeText(this, "Lütfen geçerli bir fiyat girin!", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("UrunEkle", "Ürün RecyclerView'a eklenecek: $urunAdi, Fiyat: $fiyat")
                //Manuel eklemede resmi seçilen üründen al
                val selectedUrunAdi = binding.urunAutoComplete.text.toString()
                val imageUrl = urunMap[selectedUrunAdi.lowercase(Locale.ROOT)]
                addUrunToRecyclerView(urunAdi, fiyat, imageUrl)
            }
        }

        // Fotograf seç butonu
        binding.fisEkleButton.setOnClickListener {
            Log.d("MainActivity", "fotografCekButton: onClick triggered")
            fotografSec()
        }

        // Otomatik tamamlama için TextWatcher
        binding.urunAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("MainActivity", "urunAutoComplete: beforeTextChanged: $charSequence")
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("MainActivity", "urunAutoComplete: onTextChanged: $charSequence")
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val query = charSequence.toString()
                    if (query.length >= 3) {
                        fetchAutocompleteData(query)
                    }
                }
                handler.postDelayed(searchRunnable!!, 1000)
            }

            override fun afterTextChanged(editable: Editable?) {
                Log.d("MainActivity", "urunAutoComplete: afterTextChanged: $editable")
            }
        })

        // Periyodik scraping işlemini başlat
        startPeriodicScraping()
        Log.d("MainActivity", "onCreate: Periodic scraping work started")

        // RecyclerView öğe seçimi
        binding.urunlerRecyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val child = rv.findChildViewUnder(e.x, e.y)
                if (child != null && e.action == MotionEvent.ACTION_UP) {
                    val position = rv.getChildAdapterPosition(child)
                    selectedUrun = urunList[position]
                    Log.d("MainActivity", "urunlerRecyclerView: onInterceptTouchEvent: Selected product: ${selectedUrun?.urunAdi}")
                    binding.urunAdiTextView.text = selectedUrun!!.urunAdi
                }
                return false
            }
        })

        // Bölüştür butonu
        binding.bolusturButton.setOnClickListener {
            Log.d("MainActivity", "bolusturButton: onClick triggered")
            if (selectedUrun != null) {
                val intent = Intent(this, BolusturmeActivity::class.java)
                intent.putExtra("selectedUrun", selectedUrun)
                startActivity(intent)
                Log.d("MainActivity", "bolusturButton: Starting BolusturmeActivity with selected product: ${selectedUrun?.urunAdi}")
            } else {
                Log.e("MainActivity", "bolusturButton: No product selected")
                Toast.makeText(this, "Lütfen bölüştürülecek bir ürün seçin!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun fotografSec() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    // Sonuçları işlemek için onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("MainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val selectedImageUri: Uri = data.data!!

            // Uyarı mesajını göster
            AlertDialog.Builder(this)
                .setTitle("Uyarı")
                .setMessage("Lütfen fişin sadece ürün adı ve fiyat kısmını içeren bir fotoğraf yükleyin.")
                .setPositiveButton("Tamam", null)
                .show()

            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImageUri)
            recognizeTextFromImage(bitmap)
        }
    }

    // OCR işlemini başlat
    private fun recognizeTextFromImage(bitmap: Bitmap) {
        Log.d("MainActivity", "recognizeTextFromImage: Starting OCR on image")
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("MainActivity", "recognizeTextFromImage: OCR successful, processing text")
                processText(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "recognizeTextFromImage: OCR failed", e)
                Toast.makeText(this, "OCR Hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processText(recognizedText: String) {
        Log.d("MainActivity", "processText: Processing recognized text")
        val lines = recognizedText.split("\n")
        var lastProductName: String? = null // Geçici olarak ürün adını saklamak için
        for (line in lines) {
            Log.d("MainActivity", "processText: Processing line: $line")
            val result = extractUrunVeFiyat(line)

            if (result != null) {
                if (result.fiyat == 0.0 && result.urunAdi.isNotEmpty()) {
                    // Fiyat bilgisi yoksa, bu satırı geçici ürün adı olarak sakla
                    lastProductName = result.urunAdi
                    Log.d("MainActivity", "processText: Storing temporary product name: ${result.urunAdi}")
                } else {
                    // Fiyat bilgisi varsa, ürünü ekle ve geçici ürün adını sıfırla
                    Log.d("MainActivity", "processText: Extracted product: ${result.urunAdi}, adding to list")
                    addUrunToRecyclerView(result.urunAdi, result.fiyat, result.imageUrl)
                    lastProductName = null // Ürün adı ve fiyatı işlendi, geçici adı sıfırla
                }
            } else if (lastProductName != null) {
                // Eğer bu satırda bir fiyat varsa ve önceki satırda ürün adı varsa, ürünü ekle
                val fiyatMatch = Regex("""\*(\d{1,3}(?:\.\d{3})*,\d{2})""").find(line)
                if (fiyatMatch != null) {
                    val fiyatText = fiyatMatch.groupValues[1]
                    val fiyat = try {
                        fiyatText.replace(".", "").replace(",", ".").toDouble()
                    } catch (e: NumberFormatException) {
                        Log.e("MainActivity", "processText: Error parsing price from next line: $fiyatText", e)
                        null
                    }

                    if (fiyat != null) {
                        addUrunToRecyclerView(lastProductName, fiyat)
                        lastProductName = null // Ürün adı ve fiyatı işlendi, geçici adı sıfırla
                    }
                }
            }
        }
    }

    private fun extractUrunVeFiyat(line: String): Urun? {
        Log.d("MainActivity", "extractUrunVeFiyat: Extracting product and price from line: $line")

        // Fiyat bilgisi içeren deseni tanımla (örnek: *35,00 veya *17,75)
        val fiyatRegex = Regex("""\*(\d{1,3}(?:\.\d{3})*,\d{2})""")
        val fiyatMatch = fiyatRegex.find(line)

        // Fiyat bilgisi bulunursa, sayısal formata dönüştür
        var fiyat: Double? = null
        if (fiyatMatch != null) {
            val fiyatText = fiyatMatch.groupValues[1]
            fiyat = try {
                fiyatText.replace(".", "").replace(",", ".").toDouble()
            } catch (e: NumberFormatException) {
                Log.e("MainActivity", "extractUrunVeFiyat: Error parsing price: $fiyatText", e)
                return null
            }
        }

        // Ürün adı için satırın tamamını al, fiyat bilgisini içeren kısmı atla
        var urunAdi = if (fiyatMatch != null) {
            line.substring(0, fiyatMatch.range.first).trim()
        } else {
            line.trim()
        }

        // Adet ve birim fiyat içeren satırları ayıkla (örnek: "2 X 22,00")
        val adetBirimFiyatRegex = Regex("""(\d+)\s*[xX]\s*(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})""")
        val adetBirimFiyatMatch = adetBirimFiyatRegex.find(urunAdi)

        if (adetBirimFiyatMatch != null) {
            // Ürün adından adet ve birim fiyat bilgisini temizle
            urunAdi = urunAdi.replace(adetBirimFiyatMatch.value, "").trim()
        }

        // Ürün adı boşsa ve fiyat null değilse, satırı atla
        if (urunAdi.isEmpty() && fiyat == null) {
            Log.d("MainActivity", "extractUrunVeFiyat: No product name and no price found in line: $line")
            return null
        }

        // Çok kısa veya anlamsız satırları filtrele (örnek: tek karakter, sadece sayılar, KDV oranları)
        if (urunAdi.isNotEmpty() && (urunAdi.length < 3 || urunAdi.all { it.isDigit() } || urunAdi.startsWith("%"))) {
            Log.d("MainActivity", "extractUrunVeFiyat: Skipping line with too short or invalid product name: $line")
            return null
        }

        // Eğer fiyat null ise, bu bir ürün adı satırıdır ve işleme devam etmek için null döndürülür
        if (fiyat == null) {
            Log.d("MainActivity", "extractUrunVeFiyat: No price extracted, treating line as product name only: $line")
            return Urun(urunAdi, 0.0, null.toString()) // Fiyat bilgisi yoksa, fiyatı 0.0 olarak ayarla ve imageUrl'i null bırak
        }

        // Fiyat bilgisi varsa, Urun nesnesi oluştur ve döndür
        Log.d("MainActivity", "extractUrunVeFiyat: Extracted product: $urunAdi, price: $fiyat")
        return Urun(urunAdi, fiyat, null.toString()) // imageUrl her zaman null
    }

    // RecyclerView'a ürün ekle
    private fun addUrunToRecyclerView(urunAdi: String, fiyat: Double, imageUrl: String? = null) {
        Log.d("MainActivity", "addUrunToRecyclerView: Adding product to RecyclerView: $urunAdi, Price: $fiyat, Image URL: ${imageUrl ?: "null"}")

        // Urun nesnesine imageUrl'i ekleyin (null ise boş string olarak eklenecek):
        urunList.add(Urun(urunAdi, fiyat, imageUrl ?: ""))
        urunAdapter.notifyItemInserted(urunList.size - 1)

        // Alanları temizle
        binding.fiyatEditText.text.clear()
        binding.urunAutoComplete.text.clear()

        Toast.makeText(this, "Ürün RecyclerView'a eklendi", Toast.LENGTH_SHORT).show()
    }

    // Firestore'dan veri çek
    private fun fetchAutocompleteData(query: String) {
        Log.d("Autocomplete", "Sorgu başlatıldı: $query")
        val db = FirebaseFirestore.getInstance()
        val urunlerRef = db.collection("urunler")

        val queryLower = query.lowercase(Locale.ROOT)
        val queryParts = queryLower.split(" ")

        urunlerRef
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("Autocomplete", "Sorgu başarılı, ${querySnapshot.size()} belge bulundu.")
                val autocompleteList = mutableSetOf<String>()
                urunMap.clear()

                for (document in querySnapshot.documents) {
                    val urunAdi = document.getString("urunAdi")?.lowercase(Locale.ROOT)
                    val imageUrl = document.getString("imageUrl")

                    if (!urunAdi.isNullOrEmpty() && !imageUrl.isNullOrEmpty()) {
                        val matches = queryParts.all { part -> urunAdi.contains(part) }
                        if (matches && autocompleteList.add(urunAdi)) {
                            urunMap[urunAdi] = imageUrl
                            Log.d("Autocomplete", "Eklendi: $urunAdi, Image URL: $imageUrl")
                        }
                    } else {
                        Log.w("Autocomplete", "Boş veri tespit edildi. Belge ID: ${document.id}")
                    }
                }

                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, autocompleteList.toList())
                binding.urunAutoComplete.setAdapter(adapter)
                adapter.notifyDataSetChanged()

                Log.d("Autocomplete", "Veri başarıyla bağlandı.")
            }
            .addOnFailureListener { e ->
                Log.e("Autocomplete", "Veri çekme hatası: ${e.message}")
            }
    }

    // Periyodik scraping işlemini başlat
    private fun startPeriodicScraping() {
        Log.d("MainActivity", "startPeriodicScraping: Starting periodic scraping work")
        val workRequest = PeriodicWorkRequestBuilder<ScrapeWorker>(7, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "scraping_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}