package com.example.acusen.dsp

import kotlin.math.*

/**
 * Pokročilý MFCC (Mel-frequency cepstral coefficients) procesor
 * Převádí audio signál na matematické otisky nezávislé na hlasitosti
 */
class MFCCProcessor {

    companion object {
        private const val NUM_MFCC_COEFFS = 13 // Počet MFCC koeficientů
        private const val NUM_MEL_FILTERS = 26 // Počet mel filtrů
        private const val SAMPLE_RATE = 44100 // Vzorkovací frekvence
        private const val FRAME_SIZE = 512 // Velikost rámce pro FFT
        private const val FRAME_SHIFT = 256 // Posun rámce
        private const val MIN_FREQ = 80.0 // Minimální frekvence (Hz)
        private const val MAX_FREQ = 8000.0 // Maximální frekvence (Hz)
        private const val PRE_EMPHASIS = 0.97 // Pre-emphasis faktor
    }

    private val fftAnalyzer = FFTAnalyzer()
    private val melFilterBank = createMelFilterBank()
    private val dctMatrix = createDCTMatrix()

    /**
     * Zpracuje audio signál a vrátí MFCC koeficienty
     * @param audioData Raw audio data (float array)
     * @return MFCC koeficienty (13 hodnot)
     */
    fun processMFCC(audioData: FloatArray): FloatArray {
        if (audioData.isEmpty()) return FloatArray(NUM_MFCC_COEFFS)

        // 1. Pre-emphasis filtr - zvýrazní vyšší frekvence
        val preEmphasized = applyPreEmphasis(audioData)

        // 2. Rozdělení na rámce s překryvem
        val frames = createFrames(preEmphasized)

        // 3. Aplikace okénkové funkce (Hamming)
        val windowedFrames = frames.map { applyHammingWindow(it) }

        // 4. FFT pro každý rámec
        val spectrums = windowedFrames.map { fftAnalyzer.computeMagnitudeSpectrum(it) }

        // 5. Aplikace mel filtrů
        val melSpectrum = spectrums.map { spectrum ->
            applyMelFilterBank(spectrum)
        }

        // 6. Logaritmus energie
        val logMelSpectrum = melSpectrum.map { frame ->
            frame.map { ln(maxOf(it, 1e-10f)) }.toFloatArray()
        }

        // 7. DCT transformace - získání MFCC koeficientů
        val mfccFrames = logMelSpectrum.map { applyDCT(it) }

        // 8. Průměrování přes všechny rámce
        return averageFrames(mfccFrames)
    }

    /**
     * Aplikuje pre-emphasis filtr pro zvýraznění vyšších frekvencí
     */
    private fun applyPreEmphasis(signal: FloatArray): FloatArray {
        val result = FloatArray(signal.size)
        result[0] = signal[0]

        for (i in 1 until signal.size) {
            result[i] = signal[i] - PRE_EMPHASIS.toFloat() * signal[i - 1]
        }

        return result
    }

    /**
     * Rozdělí signál na překrývající se rámce
     */
    private fun createFrames(signal: FloatArray): List<FloatArray> {
        val frames = mutableListOf<FloatArray>()
        var start = 0

        while (start + FRAME_SIZE <= signal.size) {
            val frame = FloatArray(FRAME_SIZE)
            System.arraycopy(signal, start, frame, 0, FRAME_SIZE)
            frames.add(frame)
            start += FRAME_SHIFT
        }

        return frames
    }

    /**
     * Aplikuje Hamming okénkovou funkci
     */
    private fun applyHammingWindow(frame: FloatArray): FloatArray {
        val windowed = FloatArray(frame.size)

        for (i in frame.indices) {
            val window = 0.54 - 0.46 * cos(2.0 * PI * i / (frame.size - 1))
            windowed[i] = (frame[i] * window).toFloat()
        }

        return windowed
    }

    /**
     * Vytvoří mel filtr banku
     */
    private fun createMelFilterBank(): Array<FloatArray> {
        val melFilters = Array(NUM_MEL_FILTERS) { FloatArray(FRAME_SIZE / 2 + 1) }

        // Převod Hz na mel škálu
        val melMin = hzToMel(MIN_FREQ)
        val melMax = hzToMel(MAX_FREQ)

        // Vytvoření mel frekvencí
        val melPoints = FloatArray(NUM_MEL_FILTERS + 2)
        for (i in melPoints.indices) {
            melPoints[i] = melMin + (melMax - melMin) * i / (NUM_MEL_FILTERS + 1)
        }

        // Převod zpět na Hz a pak na bin indexy
        val binPoints = IntArray(NUM_MEL_FILTERS + 2)
        for (i in binPoints.indices) {
            val freq = melToHz(melPoints[i])
            binPoints[i] = ((freq * (FRAME_SIZE + 1)) / SAMPLE_RATE).roundToInt()
        }

        // Vytvoření trojúhelníkových filtrů
        for (m in 0 until NUM_MEL_FILTERS) {
            val left = binPoints[m]
            val center = binPoints[m + 1]
            val right = binPoints[m + 2]

            for (k in left until center) {
                melFilters[m][k] = (k - left).toFloat() / (center - left)
            }

            for (k in center until right) {
                melFilters[m][k] = (right - k).toFloat() / (right - center)
            }
        }

        return melFilters
    }

    /**
     * Aplikuje mel filtr banku na spektrum
     */
    private fun applyMelFilterBank(spectrum: FloatArray): FloatArray {
        val melSpectrum = FloatArray(NUM_MEL_FILTERS)

        for (m in 0 until NUM_MEL_FILTERS) {
            var energy = 0f
            for (k in spectrum.indices) {
                if (k < melFilterBank[m].size) {
                    energy += spectrum[k] * melFilterBank[m][k]
                }
            }
            melSpectrum[m] = energy
        }

        return melSpectrum
    }

    /**
     * Vytvoří DCT transformační matici
     */
    private fun createDCTMatrix(): Array<FloatArray> {
        val dct = Array(NUM_MFCC_COEFFS) { FloatArray(NUM_MEL_FILTERS) }

        for (m in 0 until NUM_MFCC_COEFFS) {
            for (k in 0 until NUM_MEL_FILTERS) {
                dct[m][k] = (cos(PI * m * (k + 0.5) / NUM_MEL_FILTERS)).toFloat()
                if (m == 0) {
                    dct[m][k] *= sqrt(1.0 / NUM_MEL_FILTERS).toFloat()
                } else {
                    dct[m][k] *= sqrt(2.0 / NUM_MEL_FILTERS).toFloat()
                }
            }
        }

        return dct
    }

    /**
     * Aplikuje DCT transformaci
     */
    private fun applyDCT(melSpectrum: FloatArray): FloatArray {
        val mfcc = FloatArray(NUM_MFCC_COEFFS)

        for (m in 0 until NUM_MFCC_COEFFS) {
            var sum = 0f
            for (k in 0 until NUM_MEL_FILTERS) {
                if (k < melSpectrum.size) {
                    sum += dctMatrix[m][k] * melSpectrum[k]
                }
            }
            mfcc[m] = sum
        }

        return mfcc
    }

    /**
     * Průměruje MFCC koeficienty přes všechny rámce
     */
    private fun averageFrames(mfccFrames: List<FloatArray>): FloatArray {
        if (mfccFrames.isEmpty()) return FloatArray(NUM_MFCC_COEFFS)

        val averaged = FloatArray(NUM_MFCC_COEFFS)

        for (frame in mfccFrames) {
            for (i in 0 until NUM_MFCC_COEFFS) {
                if (i < frame.size) {
                    averaged[i] += frame[i]
                }
            }
        }

        // Průměrování
        val numFrames = mfccFrames.size.toFloat()
        for (i in averaged.indices) {
            averaged[i] /= numFrames
        }

        return averaged
    }

    /**
     * Převod Hz na mel škálu
     */
    private fun hzToMel(hz: Double): Float {
        return (2595.0 * log10(1.0 + hz / 700.0)).toFloat()
    }

    /**
     * Převod mel škály na Hz
     */
    private fun melToHz(mel: Float): Double {
        return 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
    }

    /**
     * Porovnání dvou MFCC vektorů pomocí euklidovské vzdálenosti
     */
    fun compareVectors(mfcc1: FloatArray, mfcc2: FloatArray): Float {
        if (mfcc1.size != mfcc2.size) return Float.MAX_VALUE

        var sum = 0f
        for (i in mfcc1.indices) {
            val diff = mfcc1[i] - mfcc2[i]
            sum += diff * diff
        }

        return sqrt(sum)
    }
}
