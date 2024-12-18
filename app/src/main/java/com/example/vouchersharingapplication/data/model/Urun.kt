package com.example.vouchersharingapplication.data.model

import android.os.Parcel
import android.os.Parcelable

data class Urun(
    val urunAdi: String = "",
    val fiyat: Double = 0.0,
    val imageUrl: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(urunAdi)
        parcel.writeDouble(fiyat)
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Urun> {
        override fun createFromParcel(parcel: Parcel): Urun {
            return Urun(parcel)
        }

        override fun newArray(size: Int): Array<Urun?> {
            return arrayOfNulls(size)
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "urunAdi" to urunAdi,
            "fiyat" to fiyat,
            "imageUrl" to imageUrl
        )
    }
}