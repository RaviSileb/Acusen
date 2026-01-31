package com.example.acusen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.acusen.data.SoundPattern
import com.example.acusen.dsp.MFCCProcessor
import com.example.acusen.dsp.FFTAnalyzer
import com.example.acusen.audio.AudioRecordingManager
import com.example.acusen.viewmodel.SoundPatternViewModel
import com.example.acusen.ui.components.MFCCGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Obrazovka pro nahrávání nových zvukových vzorů
 */
@Composable
fun RecordingScreen() {
    val viewModel: SoundPatternViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isRecording by remember { mutableStateOf(false) }
    var recordedAudio by remember { mutableStateOf<FloatArray?>(null) }
    var recordingTime by remember { mutableStateOf(0) }
    var patternName by remember { mutableStateOf("") }
    var patternDescription by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<AnalysisInfo?>(null) }

    // Timer pro nahrávání
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording && recordingTime < 10) { // Max 10 sekund
                delay(1000)
                recordingTime++
            }
            if (recordingTime >= 10) {
                isRecording = false
                // TODO: Automaticky zastavit nahrávání
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Nahrání nového vzoru",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Instrukce
        InstructionsCard()

        // Recording kontrola
        RecordingCard(
            isRecording = isRecording,
            recordingTime = recordingTime,
            hasAudio = recordedAudio != null,
            onStartRecording = {
                isRecording = true
                recordedAudio = null
                analysisResult = null
                // TODO: Spustit skutečné nahrávání
                scope.launch {
                    delay(10000) // Simulace 10 sekund nahrávání
                    recordedAudio = FloatArray(44100 * 10) { Random.nextFloat() * 0.1f } // 10 sekund audio dat
                    isRecording = false
                }
            },
            onStopRecording = {
                isRecording = false
                // TODO: Zastavit nahrávání
            },
            onDiscardRecording = {
                recordedAudio = null
                recordingTime = 0
                analysisResult = null
            }
        )

        // Audio analysis tlačítko
        recordedAudio?.let { audio ->
            AnalysisCard(
                hasAnalysis = analysisResult != null,
                isAnalyzing = isAnalyzing,
                analysisResult = analysisResult,
                onAnalyze = {
                    scope.launch {
                        isAnalyzing = true
                        analysisResult = analyzeAudio(audio)
                        isAnalyzing = false
                    }
                }
            )
        }

        // Pokud máme nahrávku, zobraz formulář
        recordedAudio?.let { audio ->
            PatternFormCard(
                name = patternName,
                description = patternDescription,
                onNameChange = { patternName = it },
                onDescriptionChange = { patternDescription = it },
                onSave = {
                    if (patternName.isNotBlank()) {
                        scope.launch {
                            isProcessing = true

                            // Zpracování audio dat
                            val fingerprint = createFingerprint(audio)

                            val pattern = SoundPattern(
                                name = patternName,
                                description = patternDescription.takeIf { it.isNotBlank() },
                                mfccFingerprint = fingerprint,
                                duration = audio.size * 1000 / 44100
                            )

                            viewModel.addPattern(pattern)

                            // Reset formuláře
                            recordedAudio = null
                            patternName = ""
                            patternDescription = ""
                            recordingTime = 0
                            analysisResult = null
                            isProcessing = false
                        }
                    }
                },
                enabled = patternName.isNotBlank() && !isProcessing,
                isProcessing = isProcessing
            )
        }

        // Info panel na konci stránky
        InfoPanel()

        // Spacer pro lepší scrollování
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Jak nahrávat vzory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            listOf(
                "Nahrajte 5-10 sekund zvuku (alarm, sirénа, pípání)",
                "Udržujte konstantní vzdálenost od zdroje",
                "Minimalizujte okolní ruch",
                "Pojmenujte vzor popisně (např. 'Požární alarm')"
            ).forEach { instruction ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    isRecording: Boolean,
    recordingTime: Int,
    hasAudio: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDiscardRecording: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recording indicator
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(100.dp),
                        strokeWidth = 8.dp
                    )
                }

                FloatingActionButton(
                    onClick = if (isRecording) onStopRecording else onStartRecording,
                    modifier = Modifier.size(80.dp),
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Clear else Icons.Default.Add,
                        contentDescription = if (isRecording) "Zastavit" else "Nahrávat",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Status text
            Text(
                text = when {
                    isRecording -> "Nahrávám... ${recordingTime}s / 10s"
                    hasAudio -> "✓ Nahrávka dokončena"
                    else -> "Klepněte pro spuštění nahrávání"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal
            )

            // Discard button pro hotovou nahrávku
            if (hasAudio && !isRecording) {
                OutlinedButton(
                    onClick = onDiscardRecording
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zahodit nahrávku")
                }
            }
        }
    }
}

@Composable
private fun PatternFormCard(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    enabled: Boolean,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pojmenujte vzor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Název vzoru *") },
                placeholder = { Text("např. Požární alarm, Sirén policie...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Popis (volitelný)") },
                placeholder = { Text("Detailní popis zvuku...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                enabled = !isProcessing
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zpracovávám...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ULOŽIT VZOR")
                }
            }
        }
    }
}

// Zjednodušená funkce pro vytvoření fingerprints (v produkci použijte skutečný DSP)
private suspend fun createFingerprint(audio: FloatArray): FloatArray {
    delay(1000) // Simulace zpracování
    return FloatArray(13) { Random.nextFloat() } // MFCC koeficienty
}

@Composable
private fun AnalysisCard(
    hasAnalysis: Boolean,
    isAnalyzing: Boolean,
    analysisResult: AnalysisInfo?,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Analýza zvuku",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = "Zpracování zvuku do matematického vzorce (MFCC fingerprint)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Button(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzuji...")
                } else {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ZPRACOVAT DO VZORCE")
                }
            }

            // Zobrazení výsledků analýzy
            analysisResult?.let { result ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Text(
                    text = "Výsledky analýzy:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                // Graf MFCC koeficientů
                MFCCGraph(
                    mfccData = result.mfccCoefficients,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(vertical = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "MFCC koeficienty:",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "${result.mfccCount} hodnot",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Spektrální centroid:",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "${result.spectralCentroid.toInt()} Hz",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Dominantní frekvence:",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "${result.dominantFreq.toInt()} Hz",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Energie signálu:",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            String.format("%.2f", result.energy),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPanel() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Technické informace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Vzorkovací frekvence:", "44,1 kHz")
                InfoRow("Bitová hloubka:", "16-bit PCM")
                InfoRow("Kanály:", "Mono")
                InfoRow("Maximální délka:", "10 sekund")
                InfoRow("Algoritmus:", "MFCC + DTW + FFT")
                InfoRow("Fingerprint:", "13 MFCC koeficientů")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Text(
                text = "Acoustic Sentinel používá pokročilé algoritmy pro převod zvuku na matematické otisky nezávislé na hlasitosti. MFCC (Mel-frequency cepstral coefficients) extrahuje charakteristické rysy zvuku, DTW (Dynamic Time Warping) umožňuje porovnání i při různém tempu a FFT analyzuje frekvence pro detekci dunění.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Data třída pro výsledky analýzy
data class AnalysisInfo(
    val mfccCount: Int,
    val mfccCoefficients: FloatArray, // MFCC koeficienty pro graf
    val spectralCentroid: Double,
    val dominantFreq: Double,
    val energy: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AnalysisInfo
        return mfccCount == other.mfccCount && mfccCoefficients.contentEquals(other.mfccCoefficients)
    }

    override fun hashCode(): Int {
        var result = mfccCount
        result = 31 * result + mfccCoefficients.contentHashCode()
        return result
    }
}

// Simulace analýzy zvuku
private suspend fun analyzeAudio(audioData: FloatArray): AnalysisInfo {
    delay(2000) // Simulace zpracování

    // Simulace MFCC koeficientů (13 hodnot mezi -2.0 a 2.0)
    val mfccCoefficients = FloatArray(13) { i ->
        when (i) {
            0 -> Random.nextFloat() * 4 - 2   // První koeficient má větší variaci
            else -> Random.nextFloat() * 2 - 1 // Ostatní jsou menší
        }
    }

    return AnalysisInfo(
        mfccCount = 13,
        mfccCoefficients = mfccCoefficients,
        spectralCentroid = Random.nextDouble(200.0, 4000.0),
        dominantFreq = Random.nextDouble(100.0, 2000.0),
        energy = Random.nextDouble(0.1, 1.0)
    )
}

