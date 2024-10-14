package com.example.vouchersharingapplication.data.model

data class Urun(
    val urunAdi: String = "", // Varsayılan değer
    val fiyat: Double = 0.0, // Varsayılan değer
    val imageUrl: String = ""
) {
    constructor() : this("", 0.0) // Parametresiz yapıcı
}

