package com.example.acusen.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.acusen.storage.PatternExportImportService
import com.example.acusen.storage.PatternStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pro export a import zvukových vzorů
 */
class ExportImportViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val patternStorage = PatternStorageManager(context)
    private val exportImportService = PatternExportImportService(context, patternStorage)

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState = _importState.asStateFlow()

    private val _availablePatternsCount = MutableStateFlow(0)
    val availablePatternsCount = _availablePatternsCount.asStateFlow()

    sealed class ExportState {
        object Idle : ExportState()
        object InProgress : ExportState()
        data class Success(val fileName: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    private val _currentImportUri = MutableStateFlow<Uri?>(null)
    val currentImportUri = _currentImportUri.asStateFlow()

    sealed class ImportState {
        object Idle : ImportState()
        object SelectingFile : ImportState()
        object AnalyzingFile : ImportState()
        data class FileAnalyzed(
            val exportInfo: PatternExportImportService.ExportData,
            val duplicates: List<String> = emptyList()
        ) : ImportState()
        object InProgress : ImportState()
        data class Success(val result: PatternExportImportService.ImportResult) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    init {
        refreshPatternCount()
    }

    /**
     * Spustí export vzorů
     */
    fun exportPatterns(outputUri: Uri) {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress

            try {
                val success = exportImportService.exportPatterns(outputUri)

                if (success) {
                    val fileName = exportImportService.generateExportFileName()
                    _exportState.value = ExportState.Success(fileName)
                } else {
                    _exportState.value = ExportState.Error("Export se nezdařil - žádné vzory k exportu")
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Chyba při exportu: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Analyzuje importní soubor
     */
    fun analyzeImportFile(inputUri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState.AnalyzingFile
            _currentImportUri.value = inputUri

            try {
                val exportInfo = exportImportService.getExportInfo(inputUri)

                if (exportInfo != null) {
                    // Kontrola duplicit
                    val existingPatterns = patternStorage.getAllPatterns()
                    val existingNames = existingPatterns.map { it.name }.toSet()
                    val duplicates = exportInfo.patterns.filter {
                        existingNames.contains(it.name)
                    }.map { it.name }

                    _importState.value = ImportState.FileAnalyzed(
                        exportInfo = exportInfo,
                        duplicates = duplicates
                    )
                } else {
                    _importState.value = ImportState.Error("Neplatný nebo poškozený export soubor")
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Chyba při analýze souboru: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Spustí import z aktuálně analyzovaného souboru
     */
    fun importFromAnalyzedFile(replaceExisting: Boolean = false) {
        val uri = _currentImportUri.value
        if (uri != null) {
            importPatterns(uri, replaceExisting)
        } else {
            _importState.value = ImportState.Error("Žádný soubor nebyl vybrán pro import")
        }
    }

    /**
     * Spustí import vzorů
     */
    fun importPatterns(inputUri: Uri, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            _importState.value = ImportState.InProgress

            try {
                val result = exportImportService.importPatterns(inputUri, replaceExisting)

                if (result.success) {
                    _importState.value = ImportState.Success(result)
                    refreshPatternCount()
                } else {
                    _importState.value = ImportState.Error(
                        result.errorMessage ?: "Import se nezdařil z neznámého důvodu"
                    )
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Chyba při importu: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Resetuje export stav
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    /**
     * Resetuje import stav
     */
    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    /**
     * Získá název souboru pro export
     */
    fun getExportFileName(): String {
        return exportImportService.generateExportFileName()
    }

    /**
     * Ověří, zda jsou k dispozici vzory pro export
     */
    fun hasPattersToExport(): Boolean {
        return _availablePatternsCount.value > 0
    }

    private fun refreshPatternCount() {
        viewModelScope.launch {
            _availablePatternsCount.value = patternStorage.getAllPatterns().size
        }
    }
}

