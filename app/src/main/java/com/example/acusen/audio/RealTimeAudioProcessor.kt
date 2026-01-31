package com.example.acusen.audio

import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.AudioFormat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.acusen.dsp.MFCCProcessor
import com.example.acusen.dsp.FFTAnalyzer
import com.example.acusen.dsp.DTWMatcher
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Real-time audio processing manager
 * Kombinuje audio nahrávání, circular buffer, fingerprinting a pattern matching
 */
class RealTimeAudioProcessor(
    private val context: Context,
    private val onPatternDetected: (String, Float, FloatArray) -> Unit
) {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 4
        private const val PROCESSING_INTERVAL_MS = 100L // 10x per second
        private const val DETECTION_WINDOW_SECONDS = 3 // Analyzovat posledních 3 sekund
    }

    private val mfccProcessor = MFCCProcessor()
    private val fftAnalyzer = FFTAnalyzer()
    private val dtwMatcher = DTWMatcher()
    private val circularBuffer = CircularAudioBuffer(SAMPLE_RATE, 15)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isProcessing = false

    // Reference patterns pro porovnání
    private val referencePatterns = mutableMapOf<String, PatternData>()

    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var recordingJob: Job? = null
    private var processingJob: Job? = null

    /**
     * Spustí real-time audio zpracování
     */
    fun startProcessing(): Boolean {
        if (!hasAudioPermission()) {
            return false
        }

        if (isProcessing) {
            return true
        }

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_MULTIPLIER

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return false
            }

            isProcessing = true
            startRecording()
            startPatternDetection()

            return true

        } catch (e: Exception) {
            stopProcessing()
            return false
        }
    }

    /**
     * Zastavuje real-time zpracování
     */
    fun stopProcessing() {
        isProcessing = false
        isRecording = false

        recordingJob?.cancel()
        processingJob?.cancel()

        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null

        circularBuffer.clear()
    }

    /**
     * Přidá referenční vzor pro detekci
     */
    fun addReferencePattern(name: String, audioData: FloatArray, isActive: Boolean = true) {
        val mfccFingerprint = mfccProcessor.processMFCC(audioData)
        val fftFeatures = fftAnalyzer.computeSpectralFeatures(audioData)
        val rumbleAnalysis = fftAnalyzer.detectLowFrequencyRumble(audioData)

        val patternData = PatternData(
            name = name,
            mfccFingerprint = mfccFingerprint,
            spectralFeatures = fftFeatures,
            rumbleAnalysis = rumbleAnalysis,
            isActive = isActive,
            audioLength = audioData.size,
            createdAt = System.currentTimeMillis()
        )

        referencePatterns[name] = patternData
    }

    /**
     * Aktualizuje aktivní stav vzoru
     */
    fun updatePatternActive(name: String, isActive: Boolean) {
        referencePatterns[name]?.let { pattern ->
            referencePatterns[name] = pattern.copy(isActive = isActive)
        }
    }

    /**
     * Odstraní referenční vzor
     */
    fun removeReferencePattern(name: String) {
        referencePatterns.remove(name)
    }

    /**
     * Získá aktivní vzory
     */
    fun getActivePatterns(): List<String> {
        return referencePatterns.filter { it.value.isActive }.keys.toList()
    }

    /**
     * Spustí audio nahrávání na pozadí
     */
    private fun startRecording() {
        recordingJob = processingScope.launch {
            val buffer = ShortArray(1024)

            audioRecord?.startRecording()
            isRecording = true

            while (isRecording && isActive) {
                val samplesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (samplesRead > 0) {
                    // Konverze Short na Float a normalizace
                    val floatBuffer = FloatArray(samplesRead)
                    for (i in 0 until samplesRead) {
                        floatBuffer[i] = buffer[i].toFloat() / 32768f
                    }

                    // Zápis do circular bufferu
                    circularBuffer.write(floatBuffer)

                    // Aplikace noise gate pro snížení šumu
                    circularBuffer.applyNoiseGate(0.005f, 20f)
                }

                yield() // Umožní cancellation
            }
        }
    }

    /**
     * Spustí detekci vzorů na pozadí
     */
    private fun startPatternDetection() {
        processingJob = processingScope.launch {
            while (isProcessing && isActive) {
                try {
                    performPatternDetection()
                    delay(PROCESSING_INTERVAL_MS)
                } catch (e: Exception) {
                    // Log error and continue
                    delay(PROCESSING_INTERVAL_MS * 2) // Longer delay on error
                }
            }
        }
    }

    /**
     * Provádí detekci vzorů v aktuálních audio datech
     */
    private suspend fun performPatternDetection() {
        // Získání posledních N sekund audio dat
        val audioData = circularBuffer.getLastSeconds(DETECTION_WINDOW_SECONDS)

        if (audioData.size < SAMPLE_RATE) { // Méně než 1 sekunda
            return
        }

        // Kontrola, zda není ticho
        if (circularBuffer.detectSilence(1)) {
            return
        }

        // Kontrola úrovně signálu - musí být dostatečně silný
        val signalLevel = circularBuffer.getSignalLevel()
        if (!signalLevel.hasSignal || signalLevel.rms < 0.01f) {
            return
        }

        // Extrakce MFCC fingerprintu z aktuálních dat
        val currentMFCC = mfccProcessor.processMFCC(audioData)

        // Spektrální analýza
        val spectralFeatures = fftAnalyzer.computeSpectralFeatures(audioData)
        val rumbleAnalysis = fftAnalyzer.detectLowFrequencyRumble(audioData)

        // Porovnání s aktivními referenčními vzory
        val activePatterns = referencePatterns.filter { it.value.isActive }

        for ((patternName, patternData) in activePatterns) {
            val dtwResult = dtwMatcher.matchPatterns(
                currentMFCC,
                patternData.mfccFingerprint,
                0.3f // Snížený threshold pro lepší detekci
            )

            // Dodatečné kontroly pro lepší přesnost
            val spectralMatch = compareSpectralFeatures(
                spectralFeatures,
                patternData.spectralFeatures
            )

            val rumbleMatch = compareRumbleAnalysis(
                rumbleAnalysis,
                patternData.rumbleAnalysis
            )

            // Kontrola energie signálu - musí být podobná
            val energyMatch = compareEnergyLevels(
                spectralFeatures.energy,
                patternData.spectralFeatures.energy
            )

            // Kombinovaná confidence s lepšími váhami
            val combinedConfidence = (
                dtwResult.confidence * 0.5f +      // 50% MFCC
                spectralMatch * 0.25f +            // 25% spektrální
                rumbleMatch * 0.15f +              // 15% dunění
                energyMatch * 0.1f                 // 10% energie
            )

            // Detekce pattern match - snížený threshold pro testování
            if (dtwResult.isMatch && combinedConfidence > 0.6f) {
                // Pattern byl detekován!
                withContext(Dispatchers.Main) {
                    onPatternDetected(patternName, combinedConfidence, audioData)
                }

                // Krátká pauza po detekci pro zamezení duplicate detekci
                delay(5000) // Prodlouženo na 5s
                break
            }
        }
    }

    /**
     * Porovná spektrální charakteristiky
     */
    private fun compareSpectralFeatures(
        current: FFTAnalyzer.SpectralFeatures,
        reference: FFTAnalyzer.SpectralFeatures
    ): Float {
        val centroidDiff = abs(current.spectralCentroid - reference.spectralCentroid) /
                          maxOf(current.spectralCentroid, reference.spectralCentroid, 1f)

        val spreadDiff = abs(current.spectralSpread - reference.spectralSpread) /
                        maxOf(current.spectralSpread, reference.spectralSpread, 1f)

        val freqDiff = abs(current.dominantFrequency - reference.dominantFrequency) /
                      maxOf(current.dominantFrequency, reference.dominantFrequency, 1f)

        val similarity = 1f - (centroidDiff + spreadDiff + freqDiff) / 3f
        return maxOf(0f, similarity)
    }

    /**
     * Porovná analýzu dunění
     */
    private fun compareRumbleAnalysis(
        current: FFTAnalyzer.RumbleAnalysis,
        reference: FFTAnalyzer.RumbleAnalysis
    ): Float {
        if (current.hasRumble != reference.hasRumble) {
            return 0.3f // Partially matching if one has rumble and other doesn't
        }

        if (!current.hasRumble && !reference.hasRumble) {
            return 1f // Perfect match if both have no rumble
        }

        // Both have rumble, compare characteristics
        val ratioSimilarity = 1f - abs(current.lowFreqRatio - reference.lowFreqRatio)
        val freqSimilarity = if (reference.peakFrequency > 0) {
            1f - abs(current.peakFrequency - reference.peakFrequency) / reference.peakFrequency
        } else 0f

        return maxOf(0f, (ratioSimilarity + freqSimilarity) / 2f)
    }

    /**
     * Porovná energetické úrovně signálu
     */
    private fun compareEnergyLevels(currentEnergy: Float, referenceEnergy: Float): Float {
        if (referenceEnergy <= 0f || currentEnergy <= 0f) return 0f

        val ratio = minOf(currentEnergy, referenceEnergy) / maxOf(currentEnergy, referenceEnergy)
        return ratio
    }

    /**
     * Získá statistiky zpracování
     */
    fun getProcessingStats(): ProcessingStats {
        val bufferStats = circularBuffer.getBufferStats()
        val signalLevel = circularBuffer.getSignalLevel()

        return ProcessingStats(
            isProcessing = isProcessing,
            isRecording = isRecording,
            activePatternCount = referencePatterns.count { it.value.isActive },
            totalPatternCount = referencePatterns.size,
            bufferUtilization = bufferStats.utilizationPercentage,
            signalLevel = signalLevel,
            sampleRate = SAMPLE_RATE
        )
    }

    /**
     * Kontroluje audio oprávnění
     */
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Data class pro uložení vzoru
     */
    data class PatternData(
        val name: String,
        val mfccFingerprint: FloatArray,
        val spectralFeatures: FFTAnalyzer.SpectralFeatures,
        val rumbleAnalysis: FFTAnalyzer.RumbleAnalysis,
        val isActive: Boolean,
        val audioLength: Int,
        val createdAt: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PatternData
            return name == other.name && mfccFingerprint.contentEquals(other.mfccFingerprint)
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + mfccFingerprint.contentHashCode()
            return result
        }
    }

    /**
     * Data class pro statistiky zpracování
     */
    data class ProcessingStats(
        val isProcessing: Boolean,
        val isRecording: Boolean,
        val activePatternCount: Int,
        val totalPatternCount: Int,
        val bufferUtilization: Float,
        val signalLevel: CircularAudioBuffer.SignalLevel,
        val sampleRate: Int
    )

    /**
     * Uvolnění zdrojů
     */
    fun release() {
        stopProcessing()
        processingScope.cancel()
    }
}
