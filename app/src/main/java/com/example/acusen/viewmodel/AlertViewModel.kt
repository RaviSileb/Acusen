package com.example.acusen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acusen.alert.AlertManager
import com.example.acusen.audio.CircularAudioBuffer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel pro správu alertů a e-mail nastavení
 */
class AlertViewModel(application: Application) : AndroidViewModel(application) {

    private val alertManager = AlertManager(application, CircularAudioBuffer())

    private val _alertSettings = MutableStateFlow(alertManager.getAlertSettings())
    val alertSettings = _alertSettings.asStateFlow()

    private val _isTestingEmail = MutableStateFlow(false)
    val isTestingEmail = _isTestingEmail.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()

    private val _lastAlertInfo = MutableStateFlow(alertManager.getLastAlertInfo())
    val lastAlertInfo = _lastAlertInfo.asStateFlow()

    /**
     * Aktualizuje nastavení alertů
     */
    fun updateAlertSettings(settings: AlertManager.AlertSettings) {
        viewModelScope.launch {
            alertManager.saveAlertSettings(settings)
            _alertSettings.value = settings
            refreshLastAlertInfo()
        }
    }

    /**
     * Spustí test e-mailu
     */
    fun testEmailAlert() {
        viewModelScope.launch {
            try {
                _isTestingEmail.value = true
                _testResult.value = null

                val success = alertManager.testAlert()

                _testResult.value = if (success) {
                    "✅ Testovací e-mail byl úspěšně odeslán!"
                } else {
                    "❌ Chyba při odesílání e-mailu. Zkontrolujte nastavení."
                }

            } catch (e: Exception) {
                _testResult.value = "❌ Chyba: ${e.message}"
            } finally {
                _isTestingEmail.value = false
            }
        }
    }

    /**
     * Aktualizuje informace o posledním alertu
     */
    fun refreshLastAlertInfo() {
        viewModelScope.launch {
            _lastAlertInfo.value = alertManager.getLastAlertInfo()
        }
    }

    /**
     * Vymaže výsledek testu
     */
    fun clearTestResult() {
        _testResult.value = null
    }

    /**
     * Získá aktuální nastavení z AlertManageru
     */
    fun refreshSettings() {
        _alertSettings.value = alertManager.getAlertSettings()
    }

    override fun onCleared() {
        super.onCleared()
        alertManager.release()
    }
}
