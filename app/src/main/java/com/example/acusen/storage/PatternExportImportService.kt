package com.example.acusen.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.acusen.data.SoundPattern
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Služba pro export a import zvukových vzorů
 */
class PatternExportImportService(
    private val context: Context,
    private val patternStorage: PatternStorageManager
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class ExportData(
        val exportedAt: Long,
        val appVersion: String,
        val totalPatterns: Int,
        val patterns: List<SoundPattern>,
        val metadata: ExportMetadata
    )

    data class ExportMetadata(
        val deviceModel: String,
        val androidVersion: String,
        val exportFormat: String = "ACUSEN_EXPORT_V1.0"
    )

    data class ImportResult(
        val success: Boolean,
        val importedCount: Int,
        val skippedCount: Int,
        val errorMessage: String? = null,
        val duplicatePatterns: List<String> = emptyList()
    )

    /**
     * Exportuje všechny vzory do ZIP souboru
     */
    suspend fun exportPatterns(outputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val patterns = patternStorage.getAllPatterns()

            if (patterns.isEmpty()) {
                return@withContext false
            }

            val exportData = ExportData(
                exportedAt = System.currentTimeMillis(),
                appVersion = "1.0.0",
                totalPatterns = patterns.size,
                patterns = patterns,
                metadata = ExportMetadata(
                    deviceModel = android.os.Build.MODEL,
                    androidVersion = android.os.Build.VERSION.RELEASE
                )
            )

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // Export JSON s daty vzorů
                    val jsonData = gson.toJson(exportData)
                    val jsonEntry = ZipEntry("patterns.json")
                    zipOut.putNextEntry(jsonEntry)
                    zipOut.write(jsonData.toByteArray())
                    zipOut.closeEntry()

                    // Export audio souborů pro každý vzor
                    patterns.forEach { pattern ->
                        if (pattern.audioFilePath != null && File(pattern.audioFilePath).exists()) {
                            val audioFile = File(pattern.audioFilePath)
                            val audioEntry = ZipEntry("audio/${pattern.id}.wav")
                            zipOut.putNextEntry(audioEntry)

                            FileInputStream(audioFile).use { audioInput ->
                                audioInput.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }

                    // Export README s informacemi
                    val readmeContent = generateExportReadme(exportData)
                    val readmeEntry = ZipEntry("README.txt")
                    zipOut.putNextEntry(readmeEntry)
                    zipOut.write(readmeContent.toByteArray())
                    zipOut.closeEntry()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Importuje vzory z ZIP souboru
     */
    suspend fun importPatterns(
        inputUri: Uri,
        replaceExisting: Boolean = false
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            var importedCount = 0
            var skippedCount = 0
            val duplicates = mutableListOf<String>()

            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry: ZipEntry?
                    var exportData: ExportData? = null
                    val audioFiles = mutableMapOf<String, ByteArray>()

                    // Čtení všech souborů ze ZIP
                    while (zipIn.nextEntry.also { entry = it } != null) {
                        val entryName = entry!!.name

                        when {
                            entryName == "patterns.json" -> {
                                val jsonContent = zipIn.readBytes().toString(Charsets.UTF_8)
                                exportData = gson.fromJson(jsonContent, ExportData::class.java)
                            }

                            entryName.startsWith("audio/") && entryName.endsWith(".wav") -> {
                                val patternId = entryName.substringAfter("audio/").substringBefore(".wav")
                                audioFiles[patternId] = zipIn.readBytes()
                            }
                        }
                        zipIn.closeEntry()
                    }

                    // Zpracování importovaných dat
                    exportData?.let { data ->
                        if (!isCompatibleFormat(data.metadata.exportFormat)) {
                            return@withContext ImportResult(
                                success = false,
                                importedCount = 0,
                                skippedCount = 0,
                                errorMessage = "Nekompatibilní formát exportu: ${data.metadata.exportFormat}"
                            )
                        }

                        val existingPatterns = patternStorage.getAllPatterns()
                        val existingNames = existingPatterns.map { it.name }.toSet()

                        data.patterns.forEach { pattern ->
                            when {
                                existingNames.contains(pattern.name) && !replaceExisting -> {
                                    duplicates.add(pattern.name)
                                    skippedCount++
                                }

                                else -> {
                                    // Import vzoru
                                    var updatedPattern = pattern.copy(
                                        id = if (replaceExisting && existingNames.contains(pattern.name)) {
                                            existingPatterns.find { it.name == pattern.name }?.id ?: UUID.randomUUID().toString()
                                        } else {
                                            UUID.randomUUID().toString()
                                        }
                                    )

                                    // Import audio souboru
                                    audioFiles[pattern.id]?.let { audioData ->
                                        val audioFile = saveImportedAudioFile(updatedPattern.id, audioData)
                                        updatedPattern = updatedPattern.copy(audioFilePath = audioFile.absolutePath)
                                    }

                                    // Uložení vzoru
                                    if (replaceExisting && existingNames.contains(pattern.name)) {
                                        patternStorage.updatePattern(updatedPattern)
                                    } else {
                                        patternStorage.addPattern(updatedPattern)
                                    }

                                    importedCount++
                                }
                            }
                        }
                    }
                }
            }

            ImportResult(
                success = true,
                importedCount = importedCount,
                skippedCount = skippedCount,
                duplicatePatterns = duplicates
            )

        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(
                success = false,
                importedCount = 0,
                skippedCount = 0,
                errorMessage = "Chyba při importu: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Generuje název souboru pro export
     */
    fun generateExportFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "AcousticSentinel_Export_$timestamp.zip"
    }

    /**
     * Získá informace o exportovaném souboru
     */
    suspend fun getExportInfo(inputUri: Uri): ExportData? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry: ZipEntry?
                    while (zipIn.nextEntry.also { entry = it } != null) {
                        if (entry!!.name == "patterns.json") {
                            val jsonContent = zipIn.readBytes().toString(Charsets.UTF_8)
                            return@withContext gson.fromJson(jsonContent, ExportData::class.java)
                        }
                        zipIn.closeEntry()
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun generateExportReadme(exportData: ExportData): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val exportTime = dateFormat.format(Date(exportData.exportedAt))

        return """
            ACOUSTIC SENTINEL - EXPORT VZORŮ
            ===================================
            
            Export vytvořen: $exportTime
            Zařízení: ${exportData.metadata.deviceModel}
            Android verze: ${exportData.metadata.androidVersion}
            Verze aplikace: ${exportData.appVersion}
            
            OBSAH EXPORTU:
            - Počet vzorů: ${exportData.totalPatterns}
            - Formát: ${exportData.metadata.exportFormat}
            
            STRUKTURA SOUBORU:
            - patterns.json - Data vzorů a konfigurace
            - audio/ - Adresář s audio soubory vzorů
            - README.txt - Tento soubor
            
            IMPORT:
            Pro import tohoto exportu použijte funkci Import v aplikaci
            Acoustic Sentinel. Export je kompatibilní s verzí aplikace 1.0+
            
            ===================================
            Acoustic Sentinel Export System
        """.trimIndent()
    }

    private fun isCompatibleFormat(format: String): Boolean {
        return format.startsWith("ACUSEN_EXPORT_V1")
    }

    private fun saveImportedAudioFile(patternId: String, audioData: ByteArray): File {
        val audioDir = File(context.filesDir, "audio_patterns")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val audioFile = File(audioDir, "${patternId}.wav")
        FileOutputStream(audioFile).use { output ->
            output.write(audioData)
        }

        return audioFile
    }
}
