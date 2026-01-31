package com.example.acusen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acusen.storage.PatternStorageManager
import com.example.acusen.data.SoundPattern
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel pro správu zvukových vzorů
 */
class SoundPatternViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = PatternStorageManager(application)

    val allPatterns = storageManager.patterns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePatterns = storageManager.activePatterns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    /**
     * Přidá nový zvukový vzor
     */
    fun addPattern(pattern: SoundPattern) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                storageManager.insertPattern(pattern)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Chyba při ukládání vzoru: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Aktualizuje existující vzor
     */
    fun updatePattern(pattern: SoundPattern) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                storageManager.updatePattern(pattern)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Chyba při aktualizaci vzoru: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Smaže vzor
     */
    fun deletePattern(pattern: SoundPattern) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                storageManager.deletePattern(pattern)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Chyba při mazání vzoru: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Přepne aktivní stav vzoru
     */
    fun togglePatternActive(patternId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                storageManager.setPatternActive(patternId, isActive)
                _errorMessage.value = null

                // Log změny pro debugging
                val pattern = storageManager.getPatternById(patternId)
                if (pattern != null) {
                    if (isActive) {
                        // Vzor byl přidán do aktivních
                        println("Vzor '${pattern.name}' byl PŘIDÁN do seznamu aktivních detekovaných vzorů")
                    } else {
                        // Vzor byl vyřazen z aktivních
                        println("Vzor '${pattern.name}' byl VYŘAZEN ze seznamu aktivních detekovaných vzorů")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Chyba při změně stavu vzoru: ${e.message}"
            }
        }
    }

    /**
     * Získá počet aktivních vzorů
     */
    fun getActivePatternCount(): Int {
        return activePatterns.value.size
    }

    /**
     * Získá seznam názvů aktivních vzorů
     */
    fun getActivePatternNames(): List<String> {
        return activePatterns.value.map { it.name }
    }

    /**
     * Vymaže všechny vzory
     */
    fun clearAllPatterns() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                storageManager.deleteAllPatterns()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Chyba při mazání všech vzorů: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Vymaže chybovou zprávu
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

