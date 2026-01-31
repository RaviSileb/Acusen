package com.example.acusen.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.acusen.data.SoundPattern
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zjednodušený storage manager pro zvukové vzory pomocí SharedPreferences
 */
class PatternStorageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sound_patterns", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _patterns = MutableStateFlow<List<SoundPattern>>(loadPatterns())
    val patterns: Flow<List<SoundPattern>> = _patterns.asStateFlow()

    private val _activePatterns = MutableStateFlow<List<SoundPattern>>(loadActivePatterns())
    val activePatterns: Flow<List<SoundPattern>> = _activePatterns.asStateFlow()

    suspend fun insertPattern(pattern: SoundPattern) {
        val currentPatterns = _patterns.value.toMutableList()
        currentPatterns.add(pattern)
        savePatterns(currentPatterns)
        _patterns.value = currentPatterns
        updateActivePatterns()
    }

    /**
     * Aktualizuje existující vzor
     */
    suspend fun updatePattern(pattern: SoundPattern) {
        val patterns = _patterns.value.toMutableList()
        val index = patterns.indexOfFirst { it.id == pattern.id }

        if (index != -1) {
            patterns[index] = pattern
            savePatterns(patterns)
            _patterns.value = patterns
        }
    }

    suspend fun deletePattern(pattern: SoundPattern) {
        val currentPatterns = _patterns.value.toMutableList()
        currentPatterns.removeAll { it.id == pattern.id }
        savePatterns(currentPatterns)
        _patterns.value = currentPatterns
        updateActivePatterns()
    }

    suspend fun setPatternActive(id: String, isActive: Boolean) {
        val currentPatterns = _patterns.value.toMutableList()
        val index = currentPatterns.indexOfFirst { it.id == id }
        if (index != -1) {
            currentPatterns[index] = currentPatterns[index].copy(isActive = isActive)
            savePatterns(currentPatterns)
            _patterns.value = currentPatterns
            updateActivePatterns()
        }
    }

    suspend fun deleteAllPatterns() {
        savePatterns(emptyList())
        _patterns.value = emptyList()
        _activePatterns.value = emptyList()
    }

    fun getPatternById(id: String): SoundPattern? {
        return _patterns.value.find { it.id == id }
    }

    private fun loadPatterns(): List<SoundPattern> {
        return try {
            val json = prefs.getString("patterns_list", null) ?: return emptyList()
            val type = object : TypeToken<List<SoundPattern>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadActivePatterns(): List<SoundPattern> {
        return loadPatterns().filter { it.isActive }
    }

    private fun savePatterns(patterns: List<SoundPattern>) {
        val json = gson.toJson(patterns)
        prefs.edit().putString("patterns_list", json).apply()
    }

    private fun updateActivePatterns() {
        _activePatterns.value = _patterns.value.filter { it.isActive }
    }

    /**
     * Získá vzor podle názvu
     */
    suspend fun getPatternByName(name: String): SoundPattern? {
        return _patterns.value.find { it.name == name }
    }

    /**
     * Získá všechny vzory jako list
     */
    fun getAllPatterns(): List<SoundPattern> {
        return _patterns.value
    }

    /**
     * Přidá nový vzor (alias pro insertPattern)
     */
    suspend fun addPattern(pattern: SoundPattern) {
        insertPattern(pattern)
    }
}
