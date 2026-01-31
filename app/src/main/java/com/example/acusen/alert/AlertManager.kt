package com.example.acusen.alert

import android.content.Context
import android.content.SharedPreferences
import com.example.acusen.data.SoundPattern
import com.example.acusen.data.AlarmDetection
import com.example.acusen.audio.CircularAudioBuffer
import com.example.acusen.storage.AlarmHistoryStorageManager
import kotlinx.coroutines.*

/**
 * Hlavní správce alertů - koordinuje email, GPS a audio data
 */
class AlertManager(
    private val context: Context,
    private val audioBuffer: CircularAudioBuffer
) {

    private val emailService = EmailAlertService(context)
    private val locationService = LocationService(context)
    private val historyStorage = AlarmHistoryStorageManager(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("acusen_alerts", Context.MODE_PRIVATE)

    private val alertScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class AlertSettings(
        val isEnabled: Boolean = false,
        val recipientEmail: String = "",
        val senderEmail: String = "",
        val senderPassword: String = "",
        val smtpHost: String = "smtp.gmail.com",
        val smtpPort: Int = 587,
        val minimumConfidence: Double = 0.7,
        val cooldownMinutes: Int = 5,
        val includeGPS: Boolean = true // Nové nastavení pro GPS
    )

    private var lastAlertTime = mutableMapOf<String, Long>()

    /**
     * Spustí alert proces pro detekovaný vzor
     */
    fun triggerAlert(pattern: SoundPattern, confidence: Double) {
        val settings = getAlertSettings()

        // Kontroly před odesláním alertu
        if (!settings.isEnabled) return
        if (confidence < settings.minimumConfidence) return
        if (isInCooldown(pattern.id, settings.cooldownMinutes)) return
        if (settings.recipientEmail.isBlank() || settings.senderEmail.isBlank()) return

        // Zaznamenání času alertu
        lastAlertTime[pattern.id] = System.currentTimeMillis()

        // Spuštění alertu v background
        alertScope.launch {
            sendAlert(pattern, confidence, settings)
        }
    }

    private suspend fun sendAlert(
        pattern: SoundPattern,
        confidence: Double,
        settings: AlertSettings
    ) {
        try {
            // Získání GPS lokace (max 10s)
            val location = if (settings.includeGPS) {
                withTimeoutOrNull(10000) {
                    locationService.getCurrentLocation()
                }
            } else null

            // Získání audio dat (posledních 15 sekund)
            val audioData = audioBuffer.getLastSeconds(15)

            if (audioData.isEmpty()) {
                // TODO: Log chybu - žádná audio data
                return
            }

            // Vytvoření záznamu detekce pro historii
            val detection = AlarmDetection(
                patternId = pattern.id,
                patternName = pattern.name,
                confidence = confidence,
                gpsLatitude = location?.latitude,
                gpsLongitude = location?.longitude,
                gpsAccuracy = location?.accuracy
            )

            // Uložení do historie
            historyStorage.addDetection(detection)

            // Vytvoření email konfigurace
            val emailConfig = EmailAlertService.AlertConfig(
                recipientEmail = settings.recipientEmail,
                senderEmail = settings.senderEmail,
                senderPassword = settings.senderPassword,
                smtpHost = settings.smtpHost,
                smtpPort = settings.smtpPort
            )

            // Odeslání e-mailu
            val success = emailService.sendAlert(
                config = emailConfig,
                patternName = pattern.name,
                confidence = confidence,
                audioData = audioData,
                location = location
            )

            if (success) {
                // TODO: Log úspěšné odeslání
                saveLastAlertInfo(pattern, confidence, location != null)
            } else {
                // TODO: Log chybu odeslání
            }

        } catch (e: Exception) {
            // TODO: Log chybu
        }
    }

    private fun isInCooldown(patternId: String, cooldownMinutes: Int): Boolean {
        val lastAlert = lastAlertTime[patternId] ?: return false
        val cooldownMs = cooldownMinutes * 60 * 1000L
        return System.currentTimeMillis() - lastAlert < cooldownMs
    }

    private fun saveLastAlertInfo(pattern: SoundPattern, confidence: Double, hasLocation: Boolean) {
        prefs.edit().apply {
            putString("last_alert_pattern", pattern.name)
            putFloat("last_alert_confidence", confidence.toFloat())
            putLong("last_alert_time", System.currentTimeMillis())
            putBoolean("last_alert_had_location", hasLocation)
            apply()
        }
    }

    fun getAlertSettings(): AlertSettings {
        return AlertSettings(
            isEnabled = prefs.getBoolean("alert_enabled", false),
            recipientEmail = prefs.getString("recipient_email", "") ?: "",
            senderEmail = prefs.getString("sender_email", "") ?: "",
            senderPassword = prefs.getString("sender_password", "") ?: "",
            smtpHost = prefs.getString("smtp_host", "smtp.gmail.com") ?: "smtp.gmail.com",
            smtpPort = prefs.getInt("smtp_port", 587),
            minimumConfidence = prefs.getFloat("min_confidence", 0.7f).toDouble(),
            cooldownMinutes = prefs.getInt("cooldown_minutes", 5),
            includeGPS = prefs.getBoolean("include_gps", true)
        )
    }

    fun saveAlertSettings(settings: AlertSettings) {
        prefs.edit().apply {
            putBoolean("alert_enabled", settings.isEnabled)
            putString("recipient_email", settings.recipientEmail)
            putString("sender_email", settings.senderEmail)
            putString("sender_password", settings.senderPassword)
            putString("smtp_host", settings.smtpHost)
            putInt("smtp_port", settings.smtpPort)
            putFloat("min_confidence", settings.minimumConfidence.toFloat())
            putInt("cooldown_minutes", settings.cooldownMinutes)
            putBoolean("include_gps", settings.includeGPS)
            apply()
        }
    }

    /**
     * Získá historii alarmů
     */
    fun getAlarmHistory() = historyStorage.alarmHistory

    /**
     * Získá posledních N záznamů historie
     */
    fun getLastDetections(count: Int) = historyStorage.getLastDetections(count)

    /**
     * Získá statistiky historie
     */
    fun getHistoryStatistics() = historyStorage.getStatistics()

    /**
     * Vymaže historii detekovaných alarmů
     */
    suspend fun clearAlarmHistory() {
        historyStorage.clearHistory()
    }

    fun getLastAlertInfo(): Map<String, Any?> {
        return mapOf(
            "pattern_name" to prefs.getString("last_alert_pattern", null),
            "confidence" to prefs.getFloat("last_alert_confidence", 0f),
            "time" to prefs.getLong("last_alert_time", 0),
            "had_location" to prefs.getBoolean("last_alert_had_location", false)
        )
    }

    /**
     * Testuje odeslání alertu
     */
    suspend fun testAlert(): Boolean {
        val settings = getAlertSettings()

        if (settings.recipientEmail.isBlank() || settings.senderEmail.isBlank()) {
            return false
        }

        return try {
            val testPattern = SoundPattern(
                name = "TEST ALERT",
                description = "Testovací alert",
                mfccFingerprint = FloatArray(13),
                duration = 1000
            )

            val location = if (settings.includeGPS) {
                locationService.getCurrentLocation(5000)
            } else null

            val testAudio = FloatArray(44100) { (Math.random() * 0.1).toFloat() } // 1 sekunda šumu

            val emailConfig = EmailAlertService.AlertConfig(
                recipientEmail = settings.recipientEmail,
                senderEmail = settings.senderEmail,
                senderPassword = settings.senderPassword,
                smtpHost = settings.smtpHost,
                smtpPort = settings.smtpPort
            )

            emailService.sendAlert(
                config = emailConfig,
                patternName = testPattern.name,
                confidence = 0.95,
                audioData = testAudio,
                location = location
            )

        } catch (e: Exception) {
            false
        }
    }

    fun release() {
        alertScope.cancel()
    }
}

