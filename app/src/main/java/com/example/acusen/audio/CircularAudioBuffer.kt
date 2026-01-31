package com.example.acusen.audio

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.*

/**
 * Pokročilý circular audio buffer pro real-time zpracování zvuku
 * Uchovává posledních N sekund audio dat v paměti pro okamžitý přístup
 */
class CircularAudioBuffer(
    private val sampleRate: Int = 44100,
    private val bufferSizeSeconds: Int = 15, // Výchozí 15 sekund
    private val channels: Int = 1 // Mono
) {

    private val bufferSize = sampleRate * bufferSizeSeconds * channels
    private val buffer = FloatArray(bufferSize)
    private var writePosition = 0
    private var isBufferFull = false
    private val lock = ReentrantReadWriteLock()

    // Statistiky pro monitoring
    private var totalSamplesWritten = 0L
    private var overflowCount = 0L
    private var lastWriteTimestamp = 0L

    /**
     * Přidá audio vzorky do circular bufferu
     */
    fun write(samples: FloatArray) {
        lock.write {
            val timestamp = System.currentTimeMillis()

            for (sample in samples) {
                buffer[writePosition] = sample
                writePosition = (writePosition + 1) % bufferSize

                // Pokud jsme prošli celý buffer jednou, označíme jako full
                if (writePosition == 0 && !isBufferFull) {
                    isBufferFull = true
                }

                totalSamplesWritten++
            }

            // Detekce overflow (příliš rychlé zápisy)
            if (lastWriteTimestamp > 0 && timestamp - lastWriteTimestamp < 10) {
                overflowCount++
            }

            lastWriteTimestamp = timestamp
        }
    }

    /**
     * Přidá jeden audio vzorek do bufferu
     */
    fun writeSample(sample: Float) {
        lock.write {
            buffer[writePosition] = sample
            writePosition = (writePosition + 1) % bufferSize

            if (writePosition == 0 && !isBufferFull) {
                isBufferFull = true
            }

            totalSamplesWritten++
        }
    }

    /**
     * Získá posledních N sekund audio dat
     */
    fun getLastSeconds(seconds: Int): FloatArray {
        return lock.read {
            val samplesRequested = min(seconds * sampleRate * channels, bufferSize)
            val result = FloatArray(samplesRequested)

            if (!isBufferFull && writePosition < samplesRequested) {
                // Buffer ještě není plný a máme méně dat než požadováno
                System.arraycopy(buffer, 0, result, 0, writePosition)
                return@read result.sliceArray(0 until writePosition)
            }

            // Výpočet start pozice
            val startPos = if (isBufferFull) {
                (writePosition - samplesRequested + bufferSize) % bufferSize
            } else {
                max(0, writePosition - samplesRequested)
            }

            // Kopírování dat s ohledem na circular povahu bufferu
            if (startPos + samplesRequested <= bufferSize) {
                // Data jsou v jednom kusu
                System.arraycopy(buffer, startPos, result, 0, samplesRequested)
            } else {
                // Data jsou rozdělena přes konec bufferu
                val firstPart = bufferSize - startPos
                val secondPart = samplesRequested - firstPart

                System.arraycopy(buffer, startPos, result, 0, firstPart)
                System.arraycopy(buffer, 0, result, firstPart, secondPart)
            }

            result
        }
    }

    /**
     * Získá audio vzorky v specifickém časovém okně
     */
    fun getTimeWindow(startSecondsAgo: Int, endSecondsAgo: Int): FloatArray {
        return lock.read {
            val startSamples = startSecondsAgo * sampleRate * channels
            val endSamples = endSecondsAgo * sampleRate * channels
            val windowSize = startSamples - endSamples

            if (windowSize <= 0) return@read FloatArray(0)

            val availableSamples = if (isBufferFull) bufferSize else writePosition
            if (startSamples > availableSamples) return@read FloatArray(0)

            val result = FloatArray(min(windowSize, availableSamples))
            val startPos = (writePosition - startSamples + bufferSize) % bufferSize

            if (startPos + result.size <= bufferSize) {
                System.arraycopy(buffer, startPos, result, 0, result.size)
            } else {
                val firstPart = bufferSize - startPos
                val secondPart = result.size - firstPart

                System.arraycopy(buffer, startPos, result, 0, firstPart)
                System.arraycopy(buffer, 0, result, firstPart, secondPart)
            }

            result
        }
    }

    /**
     * Analyzuje aktuální úroveň audio signálu
     */
    fun getSignalLevel(): SignalLevel {
        return lock.read {
            val recentSamples = getLastSeconds(1) // Posledních sekunda
            if (recentSamples.isEmpty()) {
                return@read SignalLevel(0f, 0f, 0f, false)
            }

            var sum = 0f
            var peak = 0f
            var rms = 0f

            for (sample in recentSamples) {
                val absSample = abs(sample)
                sum += absSample
                rms += sample * sample
                if (absSample > peak) peak = absSample
            }

            val average = sum / recentSamples.size
            val rmsValue = sqrt(rms / recentSamples.size)
            val hasSignal = peak > 0.01f // Práh pro detekci signálu

            SignalLevel(average, peak, rmsValue, hasSignal)
        }
    }

    /**
     * Detekuje ticho v bufferu
     */
    fun detectSilence(thresholdSeconds: Int = 3, silenceLevel: Float = 0.005f): Boolean {
        return lock.read {
            val samples = getLastSeconds(thresholdSeconds)
            if (samples.isEmpty()) return@read true

            var silentSamples = 0
            for (sample in samples) {
                if (abs(sample) <= silenceLevel) {
                    silentSamples++
                }
            }

            val silenceRatio = silentSamples.toFloat() / samples.size
            silenceRatio > 0.95f // 95% vzorků musí být ticho
        }
    }

    /**
     * Aplikuje noise gate na buffer
     */
    fun applyNoiseGate(threshold: Float = 0.01f, ratio: Float = 10f) {
        lock.write {
            for (i in buffer.indices) {
                if (abs(buffer[i]) < threshold) {
                    buffer[i] *= (1f / ratio) // Ztlumení pod prahem
                }
            }
        }
    }

    /**
     * Získá audio data s aplikovaným high-pass filtrem
     */
    fun getHighPassFiltered(cutoffFreq: Float = 100f): FloatArray {
        val samples = getLastSeconds(5) // Posledních 5 sekund
        return applyHighPassFilter(samples, cutoffFreq)
    }

    /**
     * Aplikuje high-pass filtr na audio data
     */
    private fun applyHighPassFilter(input: FloatArray, cutoffFreq: Float): FloatArray {
        if (input.isEmpty()) return input

        val output = FloatArray(input.size)
        val alpha = 1f / (1f + cutoffFreq / sampleRate)

        output[0] = input[0]
        for (i in 1 until input.size) {
            output[i] = alpha * (output[i - 1] + input[i] - input[i - 1])
        }

        return output
    }

    /**
     * Získá statistiky bufferu
     */
    fun getBufferStats(): BufferStats {
        return lock.read {
            val utilizationPercentage = if (isBufferFull) {
                100f
            } else {
                (writePosition.toFloat() / bufferSize) * 100f
            }

            val availableSeconds = if (isBufferFull) {
                bufferSizeSeconds.toFloat()
            } else {
                (writePosition.toFloat() / sampleRate / channels)
            }

            BufferStats(
                bufferSizeSeconds = bufferSizeSeconds,
                availableSeconds = availableSeconds,
                utilizationPercentage = utilizationPercentage,
                totalSamplesWritten = totalSamplesWritten,
                overflowCount = overflowCount,
                sampleRate = sampleRate,
                channels = channels,
                isBufferFull = isBufferFull
            )
        }
    }

    /**
     * Vymaže buffer
     */
    fun clear() {
        lock.write {
            buffer.fill(0f)
            writePosition = 0
            isBufferFull = false
            totalSamplesWritten = 0L
            overflowCount = 0L
        }
    }

    /**
     * Exportuje buffer do WAV formátu (teoreticky)
     */
    fun exportToWav(): ByteArray {
        val audioData = getLastSeconds(bufferSizeSeconds)
        return convertToWavBytes(audioData)
    }

    /**
     * Konvertuje float audio data na WAV bytes
     */
    private fun convertToWavBytes(audioData: FloatArray): ByteArray {
        // Zjednodušená WAV header + data
        val headerSize = 44
        val dataSize = audioData.size * 2 // 16-bit
        val totalSize = headerSize + dataSize

        val wavData = ByteArray(totalSize)
        var offset = 0

        // WAV header (zjednodušený)
        "RIFF".toByteArray().copyInto(wavData, offset); offset += 4
        intToBytes(totalSize - 8).copyInto(wavData, offset); offset += 4
        "WAVE".toByteArray().copyInto(wavData, offset); offset += 4
        "fmt ".toByteArray().copyInto(wavData, offset); offset += 4
        intToBytes(16).copyInto(wavData, offset); offset += 4 // PCM format chunk size
        shortToBytes(1).copyInto(wavData, offset); offset += 2 // Audio format (PCM)
        shortToBytes(channels.toShort()).copyInto(wavData, offset); offset += 2
        intToBytes(sampleRate).copyInto(wavData, offset); offset += 4
        intToBytes(sampleRate * channels * 2).copyInto(wavData, offset); offset += 4 // Byte rate
        shortToBytes((channels * 2).toShort()).copyInto(wavData, offset); offset += 2 // Block align
        shortToBytes(16).copyInto(wavData, offset); offset += 2 // Bits per sample
        "data".toByteArray().copyInto(wavData, offset); offset += 4
        intToBytes(dataSize).copyInto(wavData, offset); offset += 4

        // Audio data
        for (sample in audioData) {
            val intSample = (sample * 32767).toInt().coerceIn(-32768, 32767)
            shortToBytes(intSample.toShort()).copyInto(wavData, offset)
            offset += 2
        }

        return wavData
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    /**
     * Data class pro úroveň signálu
     */
    data class SignalLevel(
        val average: Float,
        val peak: Float,
        val rms: Float,
        val hasSignal: Boolean
    )

    /**
     * Data class pro statistiky bufferu
     */
    data class BufferStats(
        val bufferSizeSeconds: Int,
        val availableSeconds: Float,
        val utilizationPercentage: Float,
        val totalSamplesWritten: Long,
        val overflowCount: Long,
        val sampleRate: Int,
        val channels: Int,
        val isBufferFull: Boolean
    )
}
