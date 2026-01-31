package com.example.acusen.data

import java.util.UUID

/**
 * Data třída pro záznam v historii detekovaných alarmů
 */
data class AlarmDetection(
    val id: String = UUID.randomUUID().toString(),
    val patternId: String, // ID vzoru, který byl detekován
    val patternName: String,
    val confidence: Double, // Přesnost detekce 0.0 - 1.0
    val detectedAt: Long = System.currentTimeMillis(),
    val gpsLatitude: Double? = null, // GPS zeměpisná šířka
    val gpsLongitude: Double? = null, // GPS zeměpisná délka
    val gpsAccuracy: Float? = null, // Přesnost GPS v metrech
    val audioFilePath: String? = null // Cesta k uloženému audio souboru
) {
    /**
     * Vrací GPS lokaci jako formátovaný string
     */
    fun getGpsLocationString(): String? {
        return if (gpsLatitude != null && gpsLongitude != null) {
            val accuracyText = gpsAccuracy?.let { " (±${it.toInt()}m)" } ?: ""
            "GPS: ${String.format("%.6f", gpsLatitude)}, ${String.format("%.6f", gpsLongitude)}$accuracyText"
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

    /**
     * Vrací zkrácené GPS souřadnice pro zobrazení v UI
     */
    fun getShortGpsString(): String? {
        return if (gpsLatitude != null && gpsLongitude != null) {
            "${String.format("%.4f", gpsLatitude)}, ${String.format("%.4f", gpsLongitude)}"
        } else {
            "GPS nedostupná"
        }
    }
}
