package com.example.vouchersharingapplication.data.model

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.vouchersharingapplication.data.network.scrapeCarrefourData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.util.Locale

class ScrapeWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    init {
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        db.firestoreSettings = settings
        Log.d("ScrapeWorker", "Firestore settings initialized")
    }

    override fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()

        return try {
            runBlocking {
                // Firestore'daki mevcut ürünleri çek
                Log.d("ScrapeWorker", "doWork: Starting scraping work")
                val existingUrunler = db.collection("urunler")
                    .get()
                    .await()
                    .documents
                Log.d(
                    "ScrapeWorker",
                    "doWork: Retrieved existing products from Firestore, count: ${existingUrunler.size}"
                )

                // Ürün adlarına göre gruplandır
                val urunAdiMap = existingUrunler.groupBy { it.getString("urunAdi") }

                val batch = db.batch()

                // Kopyaları sil
                urunAdiMap.forEach { (urunAdi, documents) ->
                    if (documents.size > 1) {
                        Log.d(
                            "ScrapeWorker",
                            "doWork: Deleting duplicate documents for product: $urunAdi"
                        )
                        documents.drop(1).forEach { document ->
                            batch.delete(document.reference)
                        }
                    }
                }

                // Scraping ve veri güncelleme işlemleri
                val scrapedUrunlerList = scrapeCarrefourData()
                Log.d(
                    "ScrapeWorker",
                    "doWork: Scraped data, new product count: ${scrapedUrunlerList.size}"
                )
                val yeniUrunlerMap = scrapedUrunlerList.associateBy { it.urunAdi }

                val yeniEklenenler =
                    yeniUrunlerMap.filter { (urunAdi, _) -> !urunAdiMap.containsKey(urunAdi) }
                val guncellenenler = yeniUrunlerMap.filter { (urunAdi, yeniUrun) ->
                    urunAdiMap[urunAdi]?.let { eskiUrun ->
                        eskiUrun.first().get("fiyat") != yeniUrun.fiyat || eskiUrun.first()
                            .get("imageUrl") != yeniUrun.imageUrl
                    } ?: false
                }

                // Yeni ürünleri ekle
                yeniEklenenler.forEach { (urunAdi, yeniUrun) ->
                    Log.d("ScrapeWorker", "doWork: Adding new product: $urunAdi")
                    val documentRef = db.collection("urunler").document(urunAdi)
                    val urunMap = mapOf(
                        "urunAdi" to yeniUrun.urunAdi,
                        "urunAdiLowerCase" to yeniUrun.urunAdi.lowercase(Locale.ROOT), // Küçük harfli alanı ekle
                        "fiyat" to yeniUrun.fiyat,
                        "imageUrl" to yeniUrun.imageUrl
                    )
                    batch.set(documentRef, urunMap)
                }

                // Güncellenen ürünleri düzenle
                guncellenenler.forEach { (urunAdi, yeniUrun) ->
                    Log.d("ScrapeWorker", "doWork: Updating product: $urunAdi")
                    val documentRef = db.collection("urunler").document(urunAdi)
                    val urunMap = mapOf(
                        "urunAdi" to yeniUrun.urunAdi,
                        "urunAdiLowerCase" to yeniUrun.urunAdi.lowercase(Locale.ROOT), // Küçük harfli alanı ekle
                        "fiyat" to yeniUrun.fiyat,
                        "imageUrl" to yeniUrun.imageUrl
                    )
                    batch.set(documentRef, urunMap)
                }

                // Batch işlemini uygula
                batch.commit().await()
                Log.d("ScrapeWorker", "doWork: Batch commit successful")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("ScrapeWorker", "doWork: Error during scraping work", e)
            Result.failure()
        }
    }
}