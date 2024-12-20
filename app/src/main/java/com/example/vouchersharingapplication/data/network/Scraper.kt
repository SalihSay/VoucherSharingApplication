package com.example.vouchersharingapplication.data.network

import android.util.Log
import com.example.vouchersharingapplication.data.model.Urun
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.util.UUID

// Rastgele bir document ID oluşturmak için
fun generateRandomDocumentId(): String = UUID.randomUUID().toString()

suspend fun scrapeCarrefourData(): List<Urun> = coroutineScope {
    val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    val allProducts = mutableListOf<Urun>()
    val productNamesSet = mutableSetOf<String>() // Ürün adlarını depolamak için Set
    val categories = listOf(
        "gida", "temizlik", "kisisel-bakim", "deterjan-ve-temizlik-urunleri",
        "meyve-ve-sebze", "icecek", "sut-ve-kahvaltilik", "et-tavuk-ve-balik"
    )
    val jobList = mutableListOf<Deferred<Unit>>()
    categories.forEach { category ->
        val job = async(Dispatchers.IO) {
            var page = 1
            var hasMorePages = true
            Log.d("Scraper", "Kategori $category için sayfa taraması başlatılıyor, sayfa: $page")
            while (hasMorePages) {
                val url = "https://www.cimri.com/market/$category?magaza=carrefoursa&page=$page"
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", userAgent)
                        .build()

                    val response = withContext(Dispatchers.IO) {
                        client.newCall(request).execute()
                    }
                    val doc: Document = Jsoup.parse(response.body?.string())
                    val products: Elements = doc.select("div.ProductCard_productName__35zi5")
                    val images: Elements = doc.select("div.ProductCard_imageContainer__ASSCc img")

                    if (products.isEmpty() || images.isEmpty()) {
                        Log.d("Scraper", "$category kategorisi için sayfa $page'de ürün bulunamadı veya resimler eksik.")
                        hasMorePages = false
                    } else {
                        for (index in products.indices) {
                            val productName = products[index].text()
                            val productImageUrl = images[index].attr("src")

                            // Ürün adının eşsiz olup olmadığını kontrol et
                            if (productNamesSet.add(productName)) {
                                Log.d("Scraper", "$category kategorisi, sayfa $page'de ürün bulundu: $productName, Image URL: $productImageUrl")
                                allProducts.add(Urun(productName, 0.0, productImageUrl))
                            } else {
                                Log.d("Scraper", "$category kategorisi için mükerrer ürün bulundu ve atlandı: $productName")
                            }
                        }
                        page++
                        Log.d("Scraper", "$category kategorisi için sayfa $page taraması tamamlandı, bir sonraki sayfaya geçiliyor.")
                    }
                } catch (e: Exception) {
                    Log.e("Scraper", "Kategori $category sayfa $page için hata oluştu: ${e.message}", e)
                    hasMorePages = false
                }
            }
        }
        jobList.add(job)
    }
    jobList.awaitAll()
    Log.d("Scraper", "Tüm kategoriler için veri çekme işlemi tamamlandı. Toplam ürün sayısı: ${allProducts.size}")

    return@coroutineScope allProducts
}