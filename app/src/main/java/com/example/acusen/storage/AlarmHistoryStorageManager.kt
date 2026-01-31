package com.example.acusen.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.acusen.data.AlarmDetection
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Storage manager pro historii detekovaných alarmů
 */
class AlarmHistoryStorageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("alarm_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _alarmHistory = MutableStateFlow<List<AlarmDetection>>(loadHistory())
    val alarmHistory: Flow<List<AlarmDetection>> = _alarmHistory.asStateFlow()

    /**
     * Přidá nový záznam detekce do historie
     */
    suspend fun addDetection(detection: AlarmDetection) {
        val currentHistory = _alarmHistory.value.toMutableList()
        currentHistory.add(0, detection) // Přidat na začátek seznamu

        // Omezit na posledních 100 záznamů
        if (currentHistory.size > 100) {
            currentHistory.removeAt(currentHistory.size - 1)
        }

        saveHistory(currentHistory)
        _alarmHistory.value = currentHistory
    }

    /**
     * Smaže konkrétní detekci podle ID
     */
    suspend fun deleteDetection(detectionId: String) {
        val currentHistory = _alarmHistory.value.toMutableList()
        val updatedHistory = currentHistory.filter { it.id != detectionId }

        saveHistory(updatedHistory)
        _alarmHistory.value = updatedHistory
    }

    /**
     * Získá posledních N záznamů
     */
    fun getLastDetections(count: Int): List<AlarmDetection> {
        return _alarmHistory.value.take(count)
    }

    /**
     * Získá detekce za dnes
     */
    fun getTodayDetections(): List<AlarmDetection> {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return _alarmHistory.value.filter { it.detectedAt >= todayStart }
    }

    /**
     * Vymaže celou historii
     */
    suspend fun clearHistory() {
        saveHistory(emptyList())
        _alarmHistory.value = emptyList()
    }

    /**
     * Získá statistiky
     */
    fun getStatistics(): AlarmHistoryStats {
        val history = _alarmHistory.value
        val todayDetections = getTodayDetections()

        return AlarmHistoryStats(
            totalDetections = history.size,
            todayDetections = todayDetections.size,
            averageConfidence = if (history.isNotEmpty()) {
                history.map { it.confidence }.average()
            } else 0.0,
            detectionsWithGPS = history.count { it.gpsLatitude != null && it.gpsLongitude != null }
        )
    }

    private fun loadHistory(): List<AlarmDetection> {
        return try {
            val json = prefs.getString("history_list", null) ?: return emptyList()
            val type = object : TypeToken<List<AlarmDetection>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(history: List<AlarmDetection>) {
        val json = gson.toJson(history)
        prefs.edit().putString("history_list", json).apply()
    }
}

/**
 * Statistiky historie alarmů
 */
data class AlarmHistoryStats(
    val totalDetections: Int,
    val todayDetections: Int,
    val averageConfidence: Double,
    val detectionsWithGPS: Int
)
