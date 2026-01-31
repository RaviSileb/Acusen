package com.example.acusen.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*

/**
 * Správce audio nahrávání v real-time
 */
class AudioRecordingManager(
    private val sampleRate: Int = 44100,
    private val onAudioData: (FloatArray) -> Unit,
    private val onError: (String) -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2 // Double buffer size for safety

    /**
     * Spustí nahrávání audio
     */
    @Synchronized
    fun startRecording(): Boolean {
        if (isRecording) return true

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Nepodařilo se inicializovat AudioRecord")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            // Spustí nahrávání v background vlákně
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                recordAudio()
            }

            return true

        } catch (e: SecurityException) {
            onError("Chybí oprávnění pro nahrávání audio: ${e.message}")
            return false
        } catch (e: Exception) {
            onError("Chyba při spouštění nahrávání: ${e.message}")
            return false
        }
    }

    /**
     * Zastaví nahrávání audio
     */
    @Synchronized
    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()

        audioRecord?.let { record ->
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            } catch (e: Exception) {
                onError("Chyba při zastavování nahrávání: ${e.message}")
            }
        }

        audioRecord = null
    }

    /**
     * Kontroluje, zda probíhá nahrávání
     */
    fun isRecording(): Boolean = isRecording

    private suspend fun recordAudio() {
        val shortBuffer = ShortArray(bufferSize / 2)

        while (isRecording && !Thread.currentThread().isInterrupted) {
            try {
                val samplesRead = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0

                if (samplesRead > 0) {
                    // Převod Short na Float a normalizace
                    val floatBuffer = FloatArray(samplesRead)
                    for (i in 0 until samplesRead) {
                        floatBuffer[i] = shortBuffer[i] / 32768.0f // Normalizace na rozsah -1.0 až 1.0
                    }

                    // Předání dat callback funkci
                    withContext(Dispatchers.Main) {
                        onAudioData(floatBuffer)
                    }
                }

                // Krátká pauza pro předání kontroly jiným vláknům
                yield()

            } catch (e: Exception) {
                if (isRecording) {
                    withContext(Dispatchers.Main) {
                        onError("Chyba při čtení audio dat: ${e.message}")
                    }
                }
                break
            }
        }
    }
}
