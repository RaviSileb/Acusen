package com.example.acusen.dsp

import kotlin.math.*

/**
 * Dynamic Time Warping (DTW) matcher
 * Umožňuje porovnání zvukových sekvencí i když jsou v různém tempu
 */
class DTWMatcher {

    companion object {
        private const val DEFAULT_DISTANCE_THRESHOLD = 0.5f
        private const val WINDOW_SIZE_RATIO = 0.1f // 10% okénko pro omezení DTW cesty
    }

    /**
     * Porovná dva MFCC vzory pomocí DTW algoritmu
     * @param pattern1 První MFCC vzor (referenční)
     * @param pattern2 Druhý MFCC vzor (testovací)
     * @param threshold Práh pro rozhodnutí o shodě
     * @return DTWResult s výsledky porovnání
     */
    fun matchPatterns(
        pattern1: FloatArray,
        pattern2: FloatArray,
        threshold: Float = DEFAULT_DISTANCE_THRESHOLD
    ): DTWResult {
        if (pattern1.isEmpty() || pattern2.isEmpty()) {
            return DTWResult(
                distance = Float.MAX_VALUE,
                normalizedDistance = 1.0f,
                isMatch = false,
                confidence = 0.0f,
                pathLength = 0
            )
        }

        // Pro jednorozměrné vzory (průměrné MFCC) použijeme jednoduchou euklidovskou vzdálenost
        if (pattern1.size == pattern2.size) {
            val distance = computeEuclideanDistance(pattern1, pattern2)
            val normalizedDistance = distance / pattern1.size
            val confidence = maxOf(0f, 1f - normalizedDistance / threshold)

            return DTWResult(
                distance = distance,
                normalizedDistance = normalizedDistance,
                isMatch = normalizedDistance <= threshold,
                confidence = confidence,
                pathLength = pattern1.size
            )
        }

        // Pro různě dlouhé vzory použijeme skutečný DTW
        return computeDTW(pattern1, pattern2, threshold)
    }

    /**
     * Porovná dvourozměrné MFCC matice (čas x koeficienty)
     */
    fun matchSequences(
        sequence1: Array<FloatArray>,
        sequence2: Array<FloatArray>,
        threshold: Float = DEFAULT_DISTANCE_THRESHOLD
    ): DTWResult {
        if (sequence1.isEmpty() || sequence2.isEmpty()) {
            return DTWResult(
                distance = Float.MAX_VALUE,
                normalizedDistance = 1.0f,
                isMatch = false,
                confidence = 0.0f,
                pathLength = 0
            )
        }

        val m = sequence1.size
        val n = sequence2.size

        // DTW matice
        val dtwMatrix = Array(m + 1) { FloatArray(n + 1) { Float.MAX_VALUE } }
        dtwMatrix[0][0] = 0f

        // Výpočet lokálních vzdáleností
        val localDistances = Array(m) { FloatArray(n) }
        for (i in 0 until m) {
            for (j in 0 until n) {
                localDistances[i][j] = computeEuclideanDistance(sequence1[i], sequence2[j])
            }
        }

        // DTW výpočet s omezením cesty (Sakoe-Chiba band)
        val windowSize = maxOf(1, (maxOf(m, n) * WINDOW_SIZE_RATIO).toInt())

        for (i in 1..m) {
            val jStart = maxOf(1, i - windowSize)
            val jEnd = minOf(n, i + windowSize)

            for (j in jStart..jEnd) {
                val cost = localDistances[i - 1][j - 1]
                val insertion = dtwMatrix[i - 1][j]
                val deletion = dtwMatrix[i][j - 1]
                val match = dtwMatrix[i - 1][j - 1]

                dtwMatrix[i][j] = cost + minOf(insertion, deletion, match)
            }
        }

        val totalDistance = dtwMatrix[m][n]
        val pathLength = m + n // Aproximace délky cesty
        val normalizedDistance = totalDistance / pathLength
        val confidence = maxOf(0f, 1f - normalizedDistance / threshold)

        return DTWResult(
            distance = totalDistance,
            normalizedDistance = normalizedDistance,
            isMatch = normalizedDistance <= threshold,
            confidence = confidence,
            pathLength = pathLength
        )
    }

    /**
     * Výpočet DTW pro jednorozměrné vzory různé délky
     */
    private fun computeDTW(
        pattern1: FloatArray,
        pattern2: FloatArray,
        threshold: Float
    ): DTWResult {
        val m = pattern1.size
        val n = pattern2.size

        // DTW matice
        val dtwMatrix = Array(m + 1) { FloatArray(n + 1) { Float.MAX_VALUE } }
        dtwMatrix[0][0] = 0f

        // Omezení cesty
        val windowSize = maxOf(1, (maxOf(m, n) * WINDOW_SIZE_RATIO).toInt())

        for (i in 1..m) {
            val jStart = maxOf(1, i - windowSize)
            val jEnd = minOf(n, i + windowSize)

            for (j in jStart..jEnd) {
                val cost = abs(pattern1[i - 1] - pattern2[j - 1])
                val insertion = dtwMatrix[i - 1][j]
                val deletion = dtwMatrix[i][j - 1]
                val match = dtwMatrix[i - 1][j - 1]

                dtwMatrix[i][j] = cost + minOf(insertion, deletion, match)
            }
        }

        val totalDistance = dtwMatrix[m][n]
        val pathLength = m + n
        val normalizedDistance = totalDistance / pathLength
        val confidence = maxOf(0f, 1f - normalizedDistance / threshold)

        return DTWResult(
            distance = totalDistance,
            normalizedDistance = normalizedDistance,
            isMatch = normalizedDistance <= threshold,
            confidence = confidence,
            pathLength = pathLength
        )
    }

    /**
     * Vypočítá euklidovskou vzdálenost mezi dvěma vektory
     */
    private fun computeEuclideanDistance(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) {
            // Pokud jsou různě dlouhé, porovnáme jen kratší část
            val minSize = minOf(vector1.size, vector2.size)
            var sum = 0f

            for (i in 0 until minSize) {
                val diff = vector1[i] - vector2[i]
                sum += diff * diff
            }

            // Penalizace za rozdílné délky
            val sizeDiff = abs(vector1.size - vector2.size)
            sum += sizeDiff * 0.1f

            return sqrt(sum)
        }

        var sum = 0f
        for (i in vector1.indices) {
            val diff = vector1[i] - vector2[i]
            sum += diff * diff
        }

        return sqrt(sum)
    }

    /**
     * Najde nejlepší shodu v databázi vzorů
     */
    fun findBestMatch(
        queryPattern: FloatArray,
        referencePatterns: List<Pair<String, FloatArray>>,
        threshold: Float = DEFAULT_DISTANCE_THRESHOLD
    ): MatchResult? {
        var bestMatch: MatchResult? = null
        var bestDistance = Float.MAX_VALUE

        for ((patternName, referencePattern) in referencePatterns) {
            val dtwResult = matchPatterns(queryPattern, referencePattern, threshold)

            if (dtwResult.isMatch && dtwResult.distance < bestDistance) {
                bestDistance = dtwResult.distance
                bestMatch = MatchResult(
                    patternName = patternName,
                    confidence = dtwResult.confidence,
                    distance = dtwResult.distance,
                    normalizedDistance = dtwResult.normalizedDistance
                )
            }
        }

        return bestMatch
    }

    /**
     * Pokročilé porovnání s více metrikami
     */
    fun advancedMatch(
        pattern1: FloatArray,
        pattern2: FloatArray
    ): AdvancedMatchResult {
        val dtwResult = matchPatterns(pattern1, pattern2)

        // Cosine similarity
        val cosineSimilarity = computeCosineSimilarity(pattern1, pattern2)

        // Correlation coefficient
        val correlation = computeCorrelation(pattern1, pattern2)

        // Kombinovaná confidence
        val combinedConfidence = (dtwResult.confidence + cosineSimilarity + correlation) / 3f

        return AdvancedMatchResult(
            dtwDistance = dtwResult.distance,
            dtwConfidence = dtwResult.confidence,
            cosineSimilarity = cosineSimilarity,
            correlation = correlation,
            combinedConfidence = combinedConfidence,
            isStrongMatch = combinedConfidence > 0.7f
        )
    }

    /**
     * Vypočítá cosine similarity mezi dvěma vektory
     */
    private fun computeCosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        val minSize = minOf(vector1.size, vector2.size)
        if (minSize == 0) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in 0 until minSize) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i] * vector1[i]
            norm2 += vector2[i] * vector2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    /**
     * Vypočítá korelační koeficient
     */
    private fun computeCorrelation(vector1: FloatArray, vector2: FloatArray): Float {
        val minSize = minOf(vector1.size, vector2.size)
        if (minSize == 0) return 0f

        // Výpočet průměrů
        val mean1 = vector1.take(minSize).average().toFloat()
        val mean2 = vector2.take(minSize).average().toFloat()

        var numerator = 0f
        var sum1 = 0f
        var sum2 = 0f

        for (i in 0 until minSize) {
            val diff1 = vector1[i] - mean1
            val diff2 = vector2[i] - mean2

            numerator += diff1 * diff2
            sum1 += diff1 * diff1
            sum2 += diff2 * diff2
        }

        val denominator = sqrt(sum1 * sum2)
        return if (denominator > 0) numerator / denominator else 0f
    }

    /**
     * Data class pro výsledky DTW
     */
    data class DTWResult(
        val distance: Float,
        val normalizedDistance: Float,
        val isMatch: Boolean,
        val confidence: Float,
        val pathLength: Int
    )

    /**
     * Data class pro výsledek hledání shody
     */
    data class MatchResult(
        val patternName: String,
        val confidence: Float,
        val distance: Float,
        val normalizedDistance: Float
    )

    /**
     * Data class pro pokročilé výsledky porovnání
     */
    data class AdvancedMatchResult(
        val dtwDistance: Float,
        val dtwConfidence: Float,
        val cosineSimilarity: Float,
        val correlation: Float,
        val combinedConfidence: Float,
        val isStrongMatch: Boolean
    )
}
