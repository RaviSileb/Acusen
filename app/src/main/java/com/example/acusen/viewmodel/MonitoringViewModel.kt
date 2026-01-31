package com.example.acusen.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acusen.data.SoundPattern
import com.example.acusen.service.AcousticMonitoringService
import com.example.acusen.alert.AlertManager
import com.example.acusen.audio.CircularAudioBuffer
import com.example.acusen.storage.PatternStorageManager
import com.example.acusen.storage.AlarmHistoryStorageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel pro správu monitoring služby
 */
class MonitoringViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private var monitoringService: AcousticMonitoringService? = null
    private var isServiceBound = false

    private val alertManager = AlertManager(context, CircularAudioBuffer())
    private val patternStorage = PatternStorageManager(context)
    private val historyStorage = AlarmHistoryStorageManager(context)

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring = _isMonitoring.asStateFlow()

    private val _serviceStatus = MutableStateFlow("Nepřipojeno")
    val serviceStatus = _serviceStatus.asStateFlow()

    private val _lastDetection = MutableStateFlow<DetectionInfo?>(null)
    val lastDetection = _lastDetection.asStateFlow()

    private val _permissionStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionStatus = _permissionStatus.asStateFlow()

    // Historie alarmů
    val alarmHistory = alertManager.getAlarmHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _historyStatistics = MutableStateFlow(alertManager.getHistoryStatistics())
    val historyStatistics = _historyStatistics.asStateFlow()

    // Aktivní vzory pro detekci - seznam vzorů zahrnutých v detekci
    val activePatterns = patternStorage.activePatterns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Sledování času monitoringu
    private var monitoringStartTime: Long = 0L
    private val _monitoringDuration = MutableStateFlow(0L)
    val monitoringDuration = _monitoringDuration.asStateFlow()

    // Seznam detekovaných alarmů pro zobrazení
    val detectedAlarms = historyStorage.alarmHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    data class DetectionInfo(
        val patternName: String,
        val confidence: Double,
        val timestamp: Long = System.currentTimeMillis(),
        val gpsLatitude: Double? = null,
        val gpsLongitude: Double? = null
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AcousticMonitoringService.LocalBinder
            monitoringService = binder.getService()
            isServiceBound = true

            _serviceStatus.value = "Připojeno"
            updateMonitoringState()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            monitoringService = null
            isServiceBound = false
            _serviceStatus.value = "Odpojeno"
            _isMonitoring.value = false
        }
    }

    init {
        bindToService()
        checkPermissions()
    }

    /**
     * Spustí monitoring
     */
    fun startMonitoring() {
        viewModelScope.launch {
            if (!isServiceBound) {
                bindToService()
                return@launch
            }

            val success = monitoringService?.startMonitoring() ?: false

            if (success) {
                _isMonitoring.value = true
                _serviceStatus.value = "Monitoring aktivní"
                // Nastaví čas začátku monitoringu
                monitoringStartTime = System.currentTimeMillis()
            } else {
                _serviceStatus.value = "Chyba při spouštění - zkontrolujte oprávnění"
            }
        }
    }

    /**
     * Zastaví monitoring
     */
    fun stopMonitoring() {
        viewModelScope.launch {
            monitoringService?.stopMonitoring()
            _isMonitoring.value = false
            _serviceStatus.value = "Monitoring zastaven"
            // Resetuje čas monitoringu
            monitoringStartTime = 0L
            _monitoringDuration.value = 0L
        }
    }

    /**
     * Přepne stav monitoringu
     */
    fun toggleMonitoring() {
        if (_isMonitoring.value) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    /**
     * Aktualizuje stav oprávnení
     */
    fun updatePermissions(permissions: Map<String, Boolean>) {
        _permissionStatus.value = permissions

        // Automaticky zastaví monitoring pokud chybí klíčová oprávnění
        val requiredPermissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.FOREGROUND_SERVICE
        )

        val hasRequiredPermissions = requiredPermissions.all {
            permissions[it] == true
        }

        if (!hasRequiredPermissions && _isMonitoring.value) {
            stopMonitoring()
        }
    }

    private fun handleDetection(pattern: SoundPattern, confidence: Double) {
        viewModelScope.launch {
            // Uloží informace o detekci
            _lastDetection.value = DetectionInfo(
                patternName = pattern.name,
                confidence = confidence
            )

            // Spustí alert systém
            alertManager.triggerAlert(pattern, confidence)
        }
    }

    private fun bindToService() {
        val intent = Intent(context, AcousticMonitoringService::class.java)
        context.startService(intent) // Spustí službu
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindFromService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun updateMonitoringState() {
        // Aktualizuje UI na základě skutečného stavu služby
        viewModelScope.launch {
            // Toto by mělo být implementováno v AcousticMonitoringService
            // _isMonitoring.value = monitoringService?.isMonitoring() ?: false
        }
    }

    private fun checkPermissions() {
        val permissions = mapOf(
            android.Manifest.permission.RECORD_AUDIO to false,
            android.Manifest.permission.ACCESS_FINE_LOCATION to false,
            android.Manifest.permission.ACCESS_COARSE_LOCATION to false,
            android.Manifest.permission.FOREGROUND_SERVICE to false,
            android.Manifest.permission.POST_NOTIFICATIONS to false
        )
        _permissionStatus.value = permissions
    }

    /**
     * Získá formátované informace o posledním alertu
     */
    fun getLastAlertInfo(): Map<String, Any?> {
        return alertManager.getLastAlertInfo()
    }

    /**
     * Získá posledních N detekčí z historie
     */
    fun getLastDetections(count: Int = 5) = alertManager.getLastDetections(count)

    /**
     * Aktualizuje statistiky historie
     */
    fun refreshHistoryStatistics() {
        viewModelScope.launch {
            try {
                _historyStatistics.value = historyStorage.getStatistics()
            } catch (e: Exception) {
                // Log error - použije AlertManager jako fallback
                _historyStatistics.value = alertManager.getHistoryStatistics()
            }
        }
    }

    /**
     * Vymaže historii detekovaných alarmů
     */
    fun clearAlarmHistory() {
        viewModelScope.launch {
            try {
                // Vymazání přes storage manager
                historyStorage.clearHistory()
                // Aktualizace statistik
                _historyStatistics.value = historyStorage.getStatistics()
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    /**
     * Přidá testovací data do historie (pro debugging)
     */
    fun addTestAlarmData() {
        viewModelScope.launch {
            try {
                val testDetection = com.example.acusen.data.AlarmDetection(
                    id = java.util.UUID.randomUUID().toString(),
                    patternId = "test-pattern-id",
                    patternName = "Test Alarm",
                    confidence = 0.85,
                    detectedAt = System.currentTimeMillis(),
                    gpsLatitude = 49.2075,
                    gpsLongitude = 16.6089,
                    gpsAccuracy = 12f
                )
                historyStorage.addDetection(testDetection)
                _historyStatistics.value = historyStorage.getStatistics()
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    /**
     * Smaže konkrétní alarm z historie
     */
    fun deleteAlarmById(alarmId: String) {
        viewModelScope.launch {
            try {
                historyStorage.deleteDetection(alarmId)
                _historyStatistics.value = historyStorage.getStatistics()
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    /**
     * Získá aktuální dobu monitoringu v ms
     */
    fun getCurrentMonitoringDuration(): Long {
        return if (monitoringStartTime > 0 && _isMonitoring.value) {
            System.currentTimeMillis() - monitoringStartTime
        } else {
            0L
        }
    }

    /**
     * Aktualizuje dobu monitoringu (voláno pravidelně)
     */
    fun updateMonitoringDuration() {
        _monitoringDuration.value = getCurrentMonitoringDuration()
    }

    /**
     * Získá počet aktivních vzorů
     */
    fun getActivePatternCount(): Int {
        // Toto by mělo být synchronizováno s databází
        return 0
    }

    /**
     * Simuluje detekci alarmu pro testování
     */
    fun simulateAlarmDetection() {
        viewModelScope.launch {
            try {
                // Pokud existují aktivní vzory, použije první
                val activePatterns = patternStorage.getAllPatterns().filter { it.isActive }
                if (activePatterns.isNotEmpty()) {
                    val testPattern = activePatterns.first()

                    // Simulace detekce s vysokou confidence
                    _lastDetection.value = DetectionInfo(
                        patternName = testPattern.name,
                        confidence = 0.92
                    )

                    // Přidání záznamu do historie detekovaných alarmů
                    val testDetection = com.example.acusen.data.AlarmDetection(
                        id = java.util.UUID.randomUUID().toString(),
                        patternId = testPattern.id,
                        patternName = "[TEST] ${testPattern.name}",
                        confidence = 0.92,
                        detectedAt = System.currentTimeMillis(),
                        gpsLatitude = 49.2075, // Simulované GPS
                        gpsLongitude = 16.6089,
                        gpsAccuracy = 8f
                    )
                    historyStorage.addDetection(testDetection)

                    // Spuštění alert systému
                    alertManager.triggerAlert(testPattern, 0.92)

                    // Spuštění červené obrazovky
                    val alertIntent = com.example.acusen.AlertActivity.createIntent(
                        context,
                        testPattern.name,
                        0.92f,
                        System.currentTimeMillis()
                    )
                    alertIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(alertIntent)

                } else {
                    // Pokud nejsou žádné vzory, použije test vzor
                    _lastDetection.value = DetectionInfo(
                        patternName = "TEST ALARM",
                        confidence = 0.85
                    )

                    // Přidání záznamu do historie pro test alarm
                    val testDetection = com.example.acusen.data.AlarmDetection(
                        id = java.util.UUID.randomUUID().toString(),
                        patternId = "test-alarm-id",
                        patternName = "[TEST] Simulovaný alarm",
                        confidence = 0.85,
                        detectedAt = System.currentTimeMillis(),
                        gpsLatitude = 49.2075, // Simulované GPS
                        gpsLongitude = 16.6089,
                        gpsAccuracy = 10f
                    )
                    historyStorage.addDetection(testDetection)

                    // Spuštění červené obrazovky s test vzorem
                    val alertIntent = com.example.acusen.AlertActivity.createIntent(
                        context,
                        "TEST ALARM",
                        0.85f,
                        System.currentTimeMillis()
                    )
                    alertIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(alertIntent)
                }

                // Aktualizace statistik po přidání záznamu
                _historyStatistics.value = historyStorage.getStatistics()

            } catch (e: Exception) {
                // Log error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbindFromService()
        alertManager.release()
    }
}
