package com.example.acusen.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.acusen.R
import com.example.acusen.audio.RealTimeAudioProcessor
import com.example.acusen.classifier.SoundPatternClassifier
import com.example.acusen.alert.AlertManager
import com.example.acusen.audio.CircularAudioBuffer
import com.example.acusen.storage.PatternStorageManager
import com.example.acusen.storage.AlarmHistoryStorageManager
import kotlinx.coroutines.*

/**
 * Foreground service pro kontinuální akustický monitoring
 * Integruje všechny DSP komponenty pro real-time detekci zvuků
 */
class AcousticMonitoringService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "acoustic_monitoring_channel"
        private const val CHANNEL_NAME = "Acoustic Monitoring"
    }

    private val binder = LocalBinder()
    private var isMonitoring = false

    // DSP a audio komponenty
    private lateinit var realTimeProcessor: RealTimeAudioProcessor
    private lateinit var patternClassifier: SoundPatternClassifier
    private lateinit var alertManager: AlertManager
    private lateinit var patternStorage: PatternStorageManager
    private lateinit var historyStorage: AlarmHistoryStorageManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    inner class LocalBinder : Binder() {
        fun getService(): AcousticMonitoringService = this@AcousticMonitoringService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializace komponent
        initializeComponents()

        // Vytvoření notification channel
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MONITORING" -> startMonitoring()
            "STOP_MONITORING" -> stopMonitoring()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        realTimeProcessor.release()
        serviceScope.cancel()
    }

    /**
     * Inicializuje všechny DSP komponenty
     */
    private fun initializeComponents() {
        patternStorage = PatternStorageManager(this)
        historyStorage = AlarmHistoryStorageManager(this)
        alertManager = AlertManager(this, CircularAudioBuffer())
        patternClassifier = SoundPatternClassifier()

        // Real-time processor s callback pro detekované vzory
        realTimeProcessor = RealTimeAudioProcessor(this) { patternName, confidence, audioData ->
            onPatternDetected(patternName, confidence, audioData)
        }

        // Načtení existujících vzorů do processoru
        loadPatternsIntoProcessor()
    }

    /**
     * Načte existující vzory do real-time processoru
     */
    private fun loadPatternsIntoProcessor() {
        serviceScope.launch {
            try {
                val patterns = patternStorage.getAllPatterns()
                for (pattern in patterns) {
                    // Simulace audio dat z MFCC fingerprintu
                    val simulatedAudioData = generateAudioFromMFCC(pattern.mfccFingerprint)
                    realTimeProcessor.addReferencePattern(
                        pattern.name,
                        simulatedAudioData,
                        pattern.isActive
                    )
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    /**
     * Generuje simulovaná audio data z MFCC fingerprintu
     */
    private fun generateAudioFromMFCC(mfcc: FloatArray): FloatArray {
        // Zjednodušená inverse MFCC - v produkci by to bylo sofistikovanější
        val audioLength = 44100 * 3 // 3 sekundy
        val audioData = FloatArray(audioLength)

        for (i in audioData.indices) {
            var sample = 0f
            for (j in mfcc.indices) {
                sample += mfcc[j] * kotlin.math.sin(2.0 * kotlin.math.PI * (j + 1) * 100 * i / 44100.0).toFloat()
            }
            audioData[i] = sample * 0.1f // Normalizace
        }

        return audioData
    }

    /**
     * Spustí monitoring
     */
    fun startMonitoring(): Boolean {
        if (isMonitoring) return true

        val success = realTimeProcessor.startProcessing()
        if (success) {
            isMonitoring = true
            startForeground(NOTIFICATION_ID, createNotification())
            updateNotification("Monitoring aktivní")
        }

        return success
    }

    /**
     * Zastavuje monitoring
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        realTimeProcessor.stopProcessing()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    /**
     * Callback když je detekován vzor
     */
    private fun onPatternDetected(patternName: String, confidence: Float, audioData: FloatArray) {
        serviceScope.launch {
            try {
                // Nalezení vzoru v databázi
                val pattern = patternStorage.getPatternByName(patternName)
                if (pattern != null) {
                    // SPUŠTĚNÍ ČERVENÉ OBRAZOVKY
                    val alertIntent = com.example.acusen.AlertActivity.createIntent(
                        this@AcousticMonitoringService,
                        patternName,
                        confidence,
                        System.currentTimeMillis()
                    )
                    startActivity(alertIntent)

                    // Spuštění alert procesu (email)
                    alertManager.triggerAlert(pattern, confidence.toDouble())

                    // Aktualizace notifikace
                    updateNotification("⚠️ ALARM: $patternName (${(confidence * 100).toInt()}%)")

                    // Automatické navrácení notifikace po 10s
                    delay(10000)
                    updateNotification("Monitoring aktivní")
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    /**
     * Aktualizuje vzor v real-time processoru
     */
    fun updatePattern(patternName: String, isActive: Boolean) {
        realTimeProcessor.updatePatternActive(patternName, isActive)
    }

    /**
     * Přidá nový vzor
     */
    fun addNewPattern(patternName: String, audioData: FloatArray, isActive: Boolean = true) {
        realTimeProcessor.addReferencePattern(patternName, audioData, isActive)
    }

    /**
     * Odstraní vzor
     */
    fun removePattern(patternName: String) {
        realTimeProcessor.removeReferencePattern(patternName)
    }

    /**
     * Získá statistiky monitoring služby
     */
    fun getMonitoringStats(): MonitoringStats {
        val processingStats = realTimeProcessor.getProcessingStats()
        val alertStats = alertManager.getHistoryStatistics()

        return MonitoringStats(
            isMonitoring = isMonitoring,
            activePatternCount = processingStats.activePatternCount,
            totalDetections = alertStats.totalDetections,
            todayDetections = alertStats.todayDetections,
            bufferUtilization = processingStats.bufferUtilization,
            hasAudioSignal = processingStats.signalLevel.hasSignal
        )
    }

    /**
     * Vytvoří notification channel
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Acoustic monitoring background service"
            setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Vytvoří notifikaci pro foreground service
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Acoustic Sentinel")
            .setContentText("Monitoring připraven")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * Aktualizuje text notifikace
     */
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Acoustic Sentinel")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Získá status monitoringu
     */
    fun getMonitoringStatus(): String {
        return if (isMonitoring) "Aktivní" else "Neaktivní"
    }

    /**
     * Data class pro statistiky monitoringu
     */
    data class MonitoringStats(
        val isMonitoring: Boolean,
        val activePatternCount: Int,
        val totalDetections: Int,
        val todayDetections: Int,
        val bufferUtilization: Float,
        val hasAudioSignal: Boolean
    )
}
