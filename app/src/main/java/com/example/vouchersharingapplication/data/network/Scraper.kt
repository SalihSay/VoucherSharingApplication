package com.example.vouchersharingapplication.data.network
import com.example.vouchersharingapplication.data.model.Urun


import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

fun scrapeCarrefourData(updateUIWithProducts: (List<Urun>) -> Unit) {
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
    val allProducts = mutableListOf<Urun>() // Ürün listesini saklayacak liste

    val categories = listOf("gida", "temizlik", "kisisel-bakim", "deterjan-ve-temizlik-urunleri", "meyve-ve-sebze", "icecek", "sut-ve-kahvaltilik", "et-tavuk-ve-balik")

    CoroutineScope(Dispatchers.IO).launch {
        val jobs = categories.map { category ->
            async {
                var page = 1
                var hasMorePages = true

                while (hasMorePages) {
                    val url = "https://www.cimri.com/market/$category?magaza=carrefoursa&page=$page"
                    try {
                        val doc: Document = Jsoup.connect(url)
                            .userAgent(userAgent)
                            .timeout(10000)
                            .get()

                        val products: Elements = doc.select("div.ProductCard_productName__35zi5")
                        val images: Elements = doc.select("div.ProductCard_imageContainer__ASSCc img")

                        if (products.isEmpty() || images.isEmpty()) {
                            hasMorePages = false
                        } else {
                            for (index in products.indices) {
                                val productName = products[index].text()
                                val productImageUrl = images[index].attr("src") // Resim URL'sini al
                                allProducts.add(Urun(productName, 0.0, productImageUrl)) // Ürün ismi ve resim URL'si ile ürün nesnesini ekleyin
                            }
                            page++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        hasMorePages = false // Hata olursa döngü durur
                    }
                }
            }
        }

        jobs.awaitAll() // Tüm işlemlerin tamamlanmasını bekle

        // Tüm ürünler çekildikten sonra UI'yi güncelleyin
        withContext(Dispatchers.Main) {
            updateUIWithProducts(allProducts)
        }
    }
}

