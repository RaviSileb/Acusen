package com.example.acusen.dsp

import kotlin.math.*

/**
 * Pokročilý FFT (Fast Fourier Transform) analyzér pro spektrální analýzu
 * Detekuje dunění v pásmu 20-100 Hz a počítá spektrální charakteristiky
 */
class FFTAnalyzer {

    companion object {
        private const val LOW_FREQ_MIN = 20.0 // Minimální frekvence pro detekci dunění
        private const val LOW_FREQ_MAX = 100.0 // Maximální frekvence pro detekci dunění
        private const val SAMPLE_RATE = 44100 // Vzorkovací frekvence
    }

    /**
     * Vypočítá magnitudové spektrum pomocí FFT
     */
    fun computeMagnitudeSpectrum(signal: FloatArray): FloatArray {
        val n = signal.size
        val powerOfTwo = nextPowerOfTwo(n)

        // Doplnění nulami na mocninu 2
        val paddedSignal = FloatArray(powerOfTwo)
        System.arraycopy(signal, 0, paddedSignal, 0, n)

        // FFT výpočet
        val fftResult = fft(paddedSignal)

        // Výpočet magnitude
        val magnitude = FloatArray(powerOfTwo / 2 + 1)
        for (i in magnitude.indices) {
            val real = fftResult[2 * i]
            val imag = if (2 * i + 1 < fftResult.size) fftResult[2 * i + 1] else 0f
            magnitude[i] = sqrt(real * real + imag * imag)
        }

        return magnitude
    }

    /**
     * Detekuje dunění v nízkofrekvenčním pásmu 20-100 Hz
     */
    fun detectLowFrequencyRumble(signal: FloatArray): RumbleAnalysis {
        val spectrum = computeMagnitudeSpectrum(signal)
        val freqResolution = SAMPLE_RATE.toFloat() / (2 * spectrum.size - 2)

        // Najdení binů odpovídajících 20-100 Hz
        val lowBin = (LOW_FREQ_MIN / freqResolution).toInt()
        val highBin = (LOW_FREQ_MAX / freqResolution).toInt()

        var totalEnergy = 0f
        var peakMagnitude = 0f
        var peakFrequency = 0f

        // Analýza nízkofrekvenční energie
        for (i in lowBin..min(highBin, spectrum.size - 1)) {
            val magnitude = spectrum[i]
            totalEnergy += magnitude * magnitude

            if (magnitude > peakMagnitude) {
                peakMagnitude = magnitude
                peakFrequency = i * freqResolution
            }
        }

        // Výpočet průměrné energie v celém spektru pro porovnání
        var totalSpectrumEnergy = 0f
        for (magnitude in spectrum) {
            totalSpectrumEnergy += magnitude * magnitude
        }

        val lowFreqRatio = if (totalSpectrumEnergy > 0) {
            totalEnergy / totalSpectrumEnergy
        } else 0f

        return RumbleAnalysis(
            hasRumble = lowFreqRatio > 0.15f, // Práh pro detekci dunění
            lowFreqEnergy = totalEnergy,
            lowFreqRatio = lowFreqRatio,
            peakFrequency = peakFrequency,
            peakMagnitude = peakMagnitude
        )
    }

    /**
     * Počítá spektrální charakteristiky signálu
     */
    fun computeSpectralFeatures(signal: FloatArray): SpectralFeatures {
        val spectrum = computeMagnitudeSpectrum(signal)
        val freqResolution = SAMPLE_RATE.toFloat() / (2 * spectrum.size - 2)

        // Spektrální centroid (těžiště spektra)
        var weightedSum = 0f
        var magnitudeSum = 0f

        for (i in spectrum.indices) {
            val freq = i * freqResolution
            val magnitude = spectrum[i]
            weightedSum += freq * magnitude
            magnitudeSum += magnitude
        }

        val spectralCentroid = if (magnitudeSum > 0) weightedSum / magnitudeSum else 0f

        // Dominantní frekvence
        var maxMagnitude = 0f
        var dominantFreq = 0f

        for (i in spectrum.indices) {
            if (spectrum[i] > maxMagnitude) {
                maxMagnitude = spectrum[i]
                dominantFreq = i * freqResolution
            }
        }

        // Spektrální rozptyl (šířka spektra)
        var variance = 0f
        for (i in spectrum.indices) {
            val freq = i * freqResolution
            val magnitude = spectrum[i]
            val diff = freq - spectralCentroid
            variance += diff * diff * magnitude
        }
        val spectralSpread = if (magnitudeSum > 0) sqrt(variance / magnitudeSum) else 0f

        // Celková energie signálu
        var energy = 0f
        for (sample in signal) {
            energy += sample * sample
        }
        energy = sqrt(energy / signal.size)

        return SpectralFeatures(
            spectralCentroid = spectralCentroid,
            spectralSpread = spectralSpread,
            dominantFrequency = dominantFreq,
            energy = energy,
            spectrum = spectrum
        )
    }

    /**
     * Implementace Cooley-Tukey FFT algoritmu
     */
    private fun fft(signal: FloatArray): FloatArray {
        val n = signal.size
        if (n <= 1) return signal

        // Převod na komplexní čísla (real, imaginary)
        val complex = FloatArray(n * 2)
        for (i in signal.indices) {
            complex[2 * i] = signal[i] // Real part
            complex[2 * i + 1] = 0f    // Imaginary part
        }

        fftInPlace(complex, n)
        return complex
    }

    /**
     * In-place FFT implementace
     */
    private fun fftInPlace(complex: FloatArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j >= bit) {
                j -= bit
                bit = bit shr 1
            }
            j += bit

            if (i < j) {
                // Swap complex[i] with complex[j]
                var temp = complex[2 * i]
                complex[2 * i] = complex[2 * j]
                complex[2 * j] = temp

                temp = complex[2 * i + 1]
                complex[2 * i + 1] = complex[2 * j + 1]
                complex[2 * j + 1] = temp
            }
        }

        // FFT computation
        var length = 2
        while (length <= n) {
            val wlen = 2.0 * PI / length
            val wlenReal = cos(wlen).toFloat()
            val wlenImag = -sin(wlen).toFloat()

            var i = 0
            while (i < n) {
                var wReal = 1f
                var wImag = 0f

                for (j in 0 until length / 2) {
                    val u = i + j
                    val v = i + j + length / 2

                    val tReal = complex[2 * v] * wReal - complex[2 * v + 1] * wImag
                    val tImag = complex[2 * v] * wImag + complex[2 * v + 1] * wReal

                    complex[2 * v] = complex[2 * u] - tReal
                    complex[2 * v + 1] = complex[2 * u + 1] - tImag

                    complex[2 * u] += tReal
                    complex[2 * u + 1] += tImag

                    val tempReal = wReal * wlenReal - wImag * wlenImag
                    wImag = wReal * wlenImag + wImag * wlenReal
                    wReal = tempReal
                }

                i += length
            }

            length *= 2
        }
    }

    /**
     * Najde nejbližší mocninu 2
     */
    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) {
            power *= 2
        }
        return power
    }

    /**
     * Detekuje příčné frekvence v signálu
     */
    fun detectTransientFeatures(signal: FloatArray): TransientAnalysis {
        val spectrum = computeMagnitudeSpectrum(signal)
        val freqResolution = SAMPLE_RATE.toFloat() / (2 * spectrum.size - 2)

        // Detekce ostrých přechodů ve spektru
        var transientEnergy = 0f
        var peakCount = 0
        var avgMagnitude = spectrum.sum() / spectrum.size

        for (i in 1 until spectrum.size - 1) {
            val current = spectrum[i]
            val prev = spectrum[i - 1]
            val next = spectrum[i + 1]

            // Detekce lokálního maxima
            if (current > prev && current > next && current > avgMagnitude * 2) {
                transientEnergy += current
                peakCount++
            }
        }

        return TransientAnalysis(
            hasTransients = peakCount > 3,
            transientEnergy = transientEnergy,
            peakCount = peakCount,
            sharpness = transientEnergy / avgMagnitude
        )
    }

    /**
     * Data class pro výsledky analýzy dunění
     */
    data class RumbleAnalysis(
        val hasRumble: Boolean,
        val lowFreqEnergy: Float,
        val lowFreqRatio: Float,
        val peakFrequency: Float,
        val peakMagnitude: Float
    )

    /**
     * Data class pro spektrální charakteristiky
     */
    data class SpectralFeatures(
        val spectralCentroid: Float,
        val spectralSpread: Float,
        val dominantFrequency: Float,
        val energy: Float,
        val spectrum: FloatArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SpectralFeatures
            return spectrum.contentEquals(other.spectrum)
        }

        override fun hashCode(): Int = spectrum.contentHashCode()
    }

    /**
     * Data class pro analýzu přechodových jevů
     */
    data class TransientAnalysis(
        val hasTransients: Boolean,
        val transientEnergy: Float,
        val peakCount: Int,
        val sharpness: Float
    )
}
