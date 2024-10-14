package com.example.vouchersharingapplication.data.model

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.vouchersharingapplication.data.network.scrapeCarrefourData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking

class ScrapeWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()

        return try {
            // runBlocking ile scraping işlemini bloklayarak sonuç döndürmesini sağlıyoruz
            runBlocking {
                scrapeCarrefourData { scrapedUrunlerList ->
                    // Firestore'a yeni verileri ekle
                    scrapedUrunlerList.forEach { urun ->
                        db.collection("urunler").add(urun)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
