package com.example.vouchersharingapplication.data.model

data class Urun(
    val urunAdi: String = "", // Varsayılan değer
    val fiyat: Double = 0.0,   // Varsayılan değer
    val imageUrl: String = ""  // Varsayılan değer
) {
    // com.example.vouchersharingapplication.data.model.Urun nesnesini Map'e dönüştüren fonksiyon
    fun toMap(): Map<String, Any> {
        return mapOf(
            "urunAdi" to urunAdi,
            "fiyat" to fiyat,
            "imageUrl" to imageUrl
        )
  }
}
