package com.example.acusen.data

import java.util.UUID

/**
 * Data třída pro uložení naučeného zvukového vzoru
 */
data class SoundPattern(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val mfccFingerprint: FloatArray, // MFCC koeficienty
    val spectralFeatures: FloatArray? = null, // FFT vlastnosti pro dunění
    val duration: Int, // délka vzoru v ms
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val gpsLatitude: Double? = null, // GPS zeměpisná šířka
    val gpsLongitude: Double? = null // GPS zeměpisná délka
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SoundPattern

        if (id != other.id) return false
        if (name != other.name) return false
        if (!mfccFingerprint.contentEquals(other.mfccFingerprint)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mfccFingerprint.contentHashCode()
        return result
    }

    /**
     * Vrací GPS lokaci jako formátovaný string
     */
    fun getGpsLocationString(): String? {
        return if (gpsLatitude != null && gpsLongitude != null) {
            "GPS: ${String.format("%.6f", gpsLatitude)}, ${String.format("%.6f", gpsLongitude)}"
        } else {
            null
        }
    }

    /**
     * Vrací Google Maps odkaz
     */
    fun getGoogleMapsUrl(): String? {
        return if (gpsLatitude != null && gpsLongitude != null) {
            "https://maps.google.com/?q=$gpsLatitude,$gpsLongitude"
        } else {
            null
        }
    }
}
