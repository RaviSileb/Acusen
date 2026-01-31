package com.example.acusen.classifier

import com.example.acusen.dsp.MFCCProcessor
import com.example.acusen.dsp.FFTAnalyzer
import com.example.acusen.dsp.DTWMatcher
import com.example.acusen.data.SoundPattern
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Pokročilý klasifikátor zvukových vzorů s machine learning přístupem
 * Kombinuje MFCC, FFT a DTW pro přesnou identifikaci zvuků
 */
class SoundPatternClassifier {

    private val mfccProcessor = MFCCProcessor()
    private val fftAnalyzer = FFTAnalyzer()
    private val dtwMatcher = DTWMatcher()

    companion object {
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.75f
        private const val SEQUENCE_MATCH_THRESHOLD = 0.65f
        private const val RUMBLE_MATCH_THRESHOLD = 0.7f
        private const val MIN_PATTERN_DURATION_MS = 500 // Minimum 0.5 seconds
    }

    /**
     * Klasifikuje audio vzorky proti databázi naučených vzorů
     * @param audioSamples Raw audio data
     * @param referencePatterns Seznam referenčních vzorů
     * @return ClassificationResult s nejlepší shodou
     */
    suspend fun classifyAudio(
        audioSamples: FloatArray,
        referencePatterns: List<SoundPattern>,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): ClassificationResult = withContext(Dispatchers.Default) {

        if (audioSamples.isEmpty() || referencePatterns.isEmpty()) {
            return@withContext ClassificationResult.NoMatch
        }

        // Extrakce vlastností z input audio
        val inputFeatures = extractFeatures(audioSamples)

        // Filtrování jen aktivních vzorů
        val activePatterns = referencePatterns.filter { it.isActive }

        if (activePatterns.isEmpty()) {
            return@withContext ClassificationResult.NoMatch
        }

        // Paralelní porovnání s každým vzorem
        val matches = activePatterns.map { pattern ->
            async {
                compareWithPattern(inputFeatures, pattern, confidenceThreshold)
            }
        }.awaitAll()

        // Najdení nejlepší shody
        val bestMatch = matches
            .filterNotNull()
            .maxByOrNull { it.confidence }

        bestMatch ?: ClassificationResult.NoMatch
    }

    /**
     * Rozpoznává typ zvuku (alarm, sirén, mechanická porucha, etc.)
     */
    suspend fun recognizeSoundType(audioSamples: FloatArray): SoundTypeRecognition = withContext(Dispatchers.Default) {

        val spectralFeatures = fftAnalyzer.computeSpectralFeatures(audioSamples)
        val rumbleAnalysis = fftAnalyzer.detectLowFrequencyRumble(audioSamples)
        val transientAnalysis = fftAnalyzer.detectTransientFeatures(audioSamples)
        val mfccFeatures = mfccProcessor.processMFCC(audioSamples)

        // Analýza charakteristik pro rozpoznání typu
        val soundType = when {
            // Sirén - vysoké frekvence, periodické změny
            spectralFeatures.spectralCentroid > 2000f &&
            spectralFeatures.spectralSpread > 800f &&
            transientAnalysis.hasTransients -> SoundType.SIREN

            // Alarm - střední frekvence, ostré přechody
            spectralFeatures.dominantFrequency in 800f..3000f &&
            transientAnalysis.hasTransients &&
            transientAnalysis.sharpness > 2f -> SoundType.ALARM

            // Mechanická porucha - nízké frekvence, nepravidelnost
            rumbleAnalysis.hasRumble &&
            spectralFeatures.spectralCentroid < 500f &&
            transientAnalysis.peakCount > 5 -> SoundType.MECHANICAL_FAILURE

            // Výbuchy/rány - krátké, velmi ostré
            transientAnalysis.hasTransients &&
            transientAnalysis.sharpness > 5f &&
            spectralFeatures.energy > 0.5f -> SoundType.EXPLOSION

            // Pípání - jasná dominantní frekvence
            spectralFeatures.dominantFrequency > 1000f &&
            spectralFeatures.spectralSpread < 200f -> SoundType.BEEPING

            // Dunění - nízké frekvence
            rumbleAnalysis.hasRumble &&
            rumbleAnalysis.lowFreqRatio > 0.3f -> SoundType.RUMBLE

            else -> SoundType.UNKNOWN
        }

        val confidence = calculateTypeConfidence(soundType, spectralFeatures, rumbleAnalysis, transientAnalysis)

        SoundTypeRecognition(soundType, confidence, spectralFeatures, rumbleAnalysis)
    }

    /**
     * Adaptivní učení - zlepšuje rozpoznávání na základě zpětné vazby
     */
    fun adaptiveLearnFromFeedback(
        audioSamples: FloatArray,
        correctPatternName: String,
        isCorrectClassification: Boolean
    ) {
        // V produkční verzi by zde byla logika pro:
        // 1. Aktualizaci vah charakteristik
        // 2. Jemnější ladění thresholdů
        // 3. Přidání nových training vzorků
        // 4. Statistické sledování úspěšnosti
    }

    /**
     * Extrahuje všechny potřebné vlastnosti z audio vzorků
     */
    private suspend fun extractFeatures(audioSamples: FloatArray): AudioFeatures = withContext(Dispatchers.Default) {
        val mfccFingerprint = mfccProcessor.processMFCC(audioSamples)
        val spectralFeatures = fftAnalyzer.computeSpectralFeatures(audioSamples)
        val rumbleAnalysis = fftAnalyzer.detectLowFrequencyRumble(audioSamples)
        val transientAnalysis = fftAnalyzer.detectTransientFeatures(audioSamples)

        AudioFeatures(
            mfccFingerprint = mfccFingerprint,
            spectralFeatures = spectralFeatures,
            rumbleAnalysis = rumbleAnalysis,
            transientAnalysis = transientAnalysis,
            duration = audioSamples.size,
            energy = spectralFeatures.energy
        )
    }

    /**
     * Porovná extrahované vlastnosti s referenčním vzorem
     */
    private suspend fun compareWithPattern(
        inputFeatures: AudioFeatures,
        referencePattern: SoundPattern,
        threshold: Float
    ): ClassificationResult.Match? = withContext(Dispatchers.Default) {

        // MFCC porovnání pomocí DTW
        val dtwResult = dtwMatcher.matchPatterns(
            inputFeatures.mfccFingerprint,
            referencePattern.mfccFingerprint,
            0.5f
        )

        // Spektrální vlastnosti porovnání
        val spectralSimilarity = compareSpectralProperties(
            inputFeatures,
            referencePattern
        )

        // Porovnání délky (tolerance ±50%)
        val durationSimilarity = calculateDurationSimilarity(
            inputFeatures.duration,
            referencePattern.duration
        )

        // Kombinovaná confidence s váhami
        val combinedConfidence = (
            dtwResult.confidence * 0.5f +           // 50% MFCC
            spectralSimilarity * 0.3f +             // 30% spektrální
            durationSimilarity * 0.2f               // 20% délka
        )

        // Penalty za velmi rozdílnou energii
        val energyPenalty = calculateEnergyPenalty(inputFeatures, referencePattern)
        val finalConfidence = maxOf(0f, combinedConfidence - energyPenalty)

        if (finalConfidence >= threshold) {
            ClassificationResult.Match(
                patternName = referencePattern.name,
                confidence = finalConfidence,
                distance = dtwResult.distance,
                matchType = determineMatchType(dtwResult, spectralSimilarity, durationSimilarity)
            )
        } else null
    }

    /**
     * Porovná spektrální vlastnosti
     */
    private fun compareSpectralProperties(
        input: AudioFeatures,
        reference: SoundPattern
    ): Float {
        // Pro zjednodušení použijeme základní metriky
        // V produkci by to bylo sofistikovanější

        val energyDiff = abs(input.energy - (reference.duration.toFloat() / 44100f)) /
                        maxOf(input.energy, reference.duration.toFloat() / 44100f, 0.1f)

        return maxOf(0f, 1f - energyDiff)
    }

    /**
     * Vypočítá podobnost délky audio vzorků
     */
    private fun calculateDurationSimilarity(inputDuration: Int, referenceDuration: Int): Float {
        val ratio = minOf(inputDuration, referenceDuration).toFloat() /
                   maxOf(inputDuration, referenceDuration).toFloat()

        return if (ratio >= 0.5f) ratio else 0f // Příliš rozdílné délky = 0
    }

    /**
     * Vypočítá penalizaci za rozdílnou energii
     */
    private fun calculateEnergyPenalty(input: AudioFeatures, reference: SoundPattern): Float {
        val expectedEnergy = reference.duration.toFloat() / 44100f // Approximace
        val actualEnergy = input.energy

        val energyRatio = if (expectedEnergy > 0) actualEnergy / expectedEnergy else 1f

        return when {
            energyRatio in 0.5f..2f -> 0f      // OK range
            energyRatio < 0.2f -> 0.3f          // Příliš slabý signál
            energyRatio > 5f -> 0.2f            // Příliš silný signál
            else -> 0.1f                        // Mírná odchylka
        }
    }

    /**
     * Určí typ shody
     */
    private fun determineMatchType(
        dtwResult: DTWMatcher.DTWResult,
        spectralSimilarity: Float,
        durationSimilarity: Float
    ): MatchType {
        return when {
            dtwResult.confidence > 0.9f && spectralSimilarity > 0.85f -> MatchType.EXACT
            dtwResult.confidence > 0.75f && durationSimilarity > 0.8f -> MatchType.STRONG
            dtwResult.confidence > 0.6f -> MatchType.PARTIAL
            else -> MatchType.WEAK
        }
    }

    /**
     * Vypočítá confidence pro typ zvuku
     */
    private fun calculateTypeConfidence(
        soundType: SoundType,
        spectral: FFTAnalyzer.SpectralFeatures,
        rumble: FFTAnalyzer.RumbleAnalysis,
        transient: FFTAnalyzer.TransientAnalysis
    ): Float {
        return when (soundType) {
            SoundType.SIREN -> min(1f, spectral.spectralCentroid / 3000f +
                                      if (transient.hasTransients) 0.3f else 0f)
            SoundType.ALARM -> min(1f, transient.sharpness / 5f +
                                      if (spectral.dominantFrequency in 800f..3000f) 0.4f else 0f)
            SoundType.MECHANICAL_FAILURE -> min(1f, rumble.lowFreqRatio +
                                                    transient.peakCount / 10f)
            SoundType.RUMBLE -> rumble.lowFreqRatio
            SoundType.BEEPING -> min(1f, if (spectral.spectralSpread < 200f) 0.8f else 0.3f)
            SoundType.EXPLOSION -> min(1f, transient.sharpness / 8f + spectral.energy)
            SoundType.UNKNOWN -> 0.1f
        }
    }

    /**
     * Enum pro typy zvuků
     */
    enum class SoundType {
        SIREN,
        ALARM,
        MECHANICAL_FAILURE,
        RUMBLE,
        BEEPING,
        EXPLOSION,
        UNKNOWN
    }

    /**
     * Enum pro typy shody
     */
    enum class MatchType {
        EXACT,
        STRONG,
        PARTIAL,
        WEAK
    }

    /**
     * Data class pro audio vlastnosti
     */
    data class AudioFeatures(
        val mfccFingerprint: FloatArray,
        val spectralFeatures: FFTAnalyzer.SpectralFeatures,
        val rumbleAnalysis: FFTAnalyzer.RumbleAnalysis,
        val transientAnalysis: FFTAnalyzer.TransientAnalysis,
        val duration: Int,
        val energy: Float
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AudioFeatures
            return mfccFingerprint.contentEquals(other.mfccFingerprint)
        }

        override fun hashCode(): Int = mfccFingerprint.contentHashCode()
    }

    /**
     * Sealed class pro výsledky klasifikace
     */
    sealed class ClassificationResult {
        data class Match(
            val patternName: String,
            val confidence: Float,
            val distance: Float,
            val matchType: MatchType
        ) : ClassificationResult()

        object NoMatch : ClassificationResult()
    }

    /**
     * Data class pro rozpoznání typu zvuku
     */
    data class SoundTypeRecognition(
        val soundType: SoundType,
        val confidence: Float,
        val spectralFeatures: FFTAnalyzer.SpectralFeatures,
        val rumbleAnalysis: FFTAnalyzer.RumbleAnalysis
    )
}
