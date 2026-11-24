
package br.com.hdf.anonimizador.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hdf.anonimizador.di.DefaultDispatcher
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractionException
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractor
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import br.com.hdf.anonimizador.domain.model.AppliedManualTextReplacementCommand
import br.com.hdf.anonimizador.domain.model.ChatPlatform
import br.com.hdf.anonimizador.domain.model.DocumentSource
import br.com.hdf.anonimizador.domain.model.ManualTextReplacementCommand
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.usecase.ApplyManualTextReplacementsUseCase
import br.com.hdf.anonimizador.domain.usecase.AnonymizeContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val INPUT_DEBOUNCE_MS = 300L

@HiltViewModel
class AnonymizerViewModel @Inject constructor(
    private val anonymizeContentUseCase: AnonymizeContentUseCase,
    private val applyManualTextReplacementsUseCase: ApplyManualTextReplacementsUseCase,
    private val documentExtractor: DocumentExtractor,
    @DefaultDispatcher private val computationDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnonymizerUiState())

    val uiState: StateFlow<AnonymizerUiState> =
        _uiState.asStateFlow()

    private var processingJob: Job? = null
    private var extractionJob: Job? = null
    private val undoHistory = mutableListOf<AppliedManualTextReplacementCommand>()
    private val redoHistory = mutableListOf<AppliedManualTextReplacementCommand>()

    // -------------------------------------------------
    // Entrada de texto
    // -------------------------------------------------

    fun onInputTextChanged(text: String) {
        extractionJob?.cancel()
        extractionJob = null

        updateInputText(
            text = text,
            debounceMs = INPUT_DEBOUNCE_MS
        )
    }

    private fun updateInputText(
        text: String,
        debounceMs: Long
    ) {
        if (_uiState.value.inputText == text) return

        undoHistory.clear()
        redoHistory.clear()
        _uiState.update {
            it.copy(
                inputText = text,
                errorMessage = null,
                sanitizedText = "",
                entityCounts = emptyMap(),
                removedNoiseCount = 0,
                manuallyRemovedCount = 0,
                isProcessing = false,
                canUndoManualReplacement = false,
                canRedoManualReplacement = false
            )
        }

        scheduleAnonymization(debounceMs)
    }

    // -------------------------------------------------
    // Configurações
    // -------------------------------------------------

    fun onTogglePiiType(type: PiiType) {
        _uiState.update { current ->
            val updatedTypes = current.enabledTypes.toMutableSet()

            if (!updatedTypes.add(type)) {
                updatedTypes.remove(type)
            }

            current.copy(
                enabledTypes = updatedTypes
            )
        }

        scheduleAnonymization(debounceMs = 0L)
    }

    fun replaceSelectedResult(
        target: String,
        replacement: String,
        occurrenceIndex: Int
    ) {
        if (target.isEmpty() || _uiState.value.isProcessing || occurrenceIndex < 0) return

        applyManualReplacement(
            command = ManualTextReplacementCommand.ReplaceOccurrence(
                target = target,
                replacement = replacement,
                occurrenceIndex = occurrenceIndex
            ),
            notFoundMessage = "O trecho selecionado não foi encontrado no resultado."
        )
    }

    fun replaceAllInResult(
        target: String,
        replacement: String
    ) {
        if (target.isEmpty() || _uiState.value.isProcessing) return

        applyManualReplacement(
            command = ManualTextReplacementCommand.ReplaceAll(
                target = target,
                replacement = replacement
            ),
            notFoundMessage = "O texto informado não foi encontrado no resultado."
        )
    }

    fun replaceFirstInResult(
        target: String,
        replacement: String
    ) {
        if (target.isEmpty() || _uiState.value.isProcessing) return

        applyManualReplacement(
            command = ManualTextReplacementCommand.ReplaceOccurrence(
                target = target,
                replacement = replacement,
                occurrenceIndex = 0
            ),
            notFoundMessage = "O texto informado não foi encontrado no resultado."
        )
    }

    fun undoManualReplacement() {
        if (_uiState.value.isProcessing || undoHistory.isEmpty()) return

        val command = undoHistory.last()
        val currentText = _uiState.value.sanitizedText
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        processingJob = viewModelScope.launch {
            try {
                val restoredText = withContext(computationDispatcher) {
                    command.undo(currentText)
                }
                currentCoroutineContext().ensureActive()

                if (restoredText == null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Não foi possível desfazer a última substituição."
                        )
                    }
                    return@launch
                }

                undoHistory.removeAt(undoHistory.lastIndex)
                redoHistory.add(command)
                updateHistoryAvailability()
                _uiState.update {
                    it.copy(
                        sanitizedText = restoredText,
                        isProcessing = false,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível desfazer a última substituição."
                    )
                }
            }
        }
    }

    fun redoManualReplacement() {
        if (_uiState.value.isProcessing || redoHistory.isEmpty()) return

        val command = redoHistory.last()
        val currentText = _uiState.value.sanitizedText
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        processingJob = viewModelScope.launch {
            try {
                val result = withContext(computationDispatcher) {
                    applyManualTextReplacementsUseCase(currentText, listOf(command.command))
                }
                currentCoroutineContext().ensureActive()

                val redoneCommand = result.appliedCommands.singleOrNull()
                if (redoneCommand == null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "O trecho da substituição não está mais disponível para refazer."
                        )
                    }
                    return@launch
                }

                redoHistory.removeAt(redoHistory.lastIndex)
                undoHistory.add(redoneCommand)
                updateHistoryAvailability()
                _uiState.update {
                    it.copy(
                        sanitizedText = result.text,
                        isProcessing = false,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível refazer a substituição."
                    )
                }
            }
        }
    }

    fun onEnabledTypesChanged(types: Set<PiiType>) {
        val currentTypes = _uiState.value.enabledTypes

        if (currentTypes == types) return

        _uiState.update {
            it.copy(
                enabledTypes = types.toSet()
            )
        }

        scheduleAnonymization(debounceMs = 0L)
    }

    fun onMaskingModeChanged(mode: MaskMode) {
        if (_uiState.value.maskingMode == mode) return

        _uiState.update {
            it.copy(maskingMode = mode)
        }

        scheduleAnonymization(debounceMs = 0L)
    }

    fun onToggleWhatsAppNoise(enabled: Boolean) {
        if (_uiState.value.cleanWhatsAppNoise == enabled) return

        _uiState.update {
            it.copy(cleanWhatsAppNoise = enabled)
        }

        scheduleAnonymization(debounceMs = 0L)
    }

    fun onSettingsApplied(
        enabledTypes: Set<PiiType>,
        maskingMode: MaskMode,
        cleanWhatsAppNoise: Boolean
    ) {
        val current = _uiState.value
        if (
            current.enabledTypes == enabledTypes &&
            current.maskingMode == maskingMode &&
            current.cleanWhatsAppNoise == cleanWhatsAppNoise
        ) {
            return
        }

        _uiState.update {
            it.copy(
                enabledTypes = enabledTypes.toSet(),
                maskingMode = maskingMode,
                cleanWhatsAppNoise = cleanWhatsAppNoise
            )
        }

        scheduleAnonymization(debounceMs = 0L)
    }

    // -------------------------------------------------
    // Seleção de arquivo
    // -------------------------------------------------

    fun onFileSelected(source: DocumentSource) {
        processingJob?.cancel()
        processingJob = null

        extractionJob?.cancel()

        extractionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    errorMessage = null
                )
            }

            try {
                val extractedText = withContext(computationDispatcher) {
                    documentExtractor.extractText(source)
                }

                currentCoroutineContext().ensureActive()

                extractionJob = null

                updateInputText(
                    text = extractedText,
                    debounceMs = 0L
                )

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: DocumentExtractionException) {
                currentCoroutineContext().ensureActive()

                extractionJob = null

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = mapExtractionError(exception)
                    )
                }

            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                extractionJob = null

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível ler o arquivo selecionado. Tente novamente."
                    )
                }
            }
        }
    }

    // -------------------------------------------------
    // Anonimização
    // -------------------------------------------------

    private fun scheduleAnonymization(debounceMs: Long) {
        processingJob?.cancel()
        processingJob = null

        val snapshot = _uiState.value

        if (snapshot.inputText.isBlank()) {
            undoHistory.clear()
            redoHistory.clear()
            _uiState.update {
                it.copy(
                    sanitizedText = "",
                    entityCounts = emptyMap(),
                    removedNoiseCount = 0,
                    manuallyRemovedCount = 0,
                    isProcessing = false,
                    errorMessage = null,
                    canUndoManualReplacement = false,
                    canRedoManualReplacement = false
                )
            }
            return
        }

        val activeCommands = undoHistory.map { it.command }
        val pendingRedoCommands = redoHistory.asReversed().map { it.command }

        processingJob = viewModelScope.launch {
            try {
                if (debounceMs > 0L) {
                    delay(debounceMs.milliseconds)
                }

                currentCoroutineContext().ensureActive()

                _uiState.update {
                    it.copy(isProcessing = true)
                }

                val request = AnonymizeRequest(
                    rawText = snapshot.inputText,
                    enabledTypes = snapshot.enabledTypes,
                    mode = snapshot.maskingMode,
                    noiseCleanupPlatforms = if (snapshot.cleanWhatsAppNoise) {
                        setOf(ChatPlatform.WHATSAPP)
                    } else {
                        emptySet()
                    }
                )

                val output = withContext(computationDispatcher) {
                    anonymizeContentUseCase(request)
                }

                currentCoroutineContext().ensureActive()

                val (replacementResult, rebasedRedoHistory) =
                    withContext(computationDispatcher) {
                        val appliedManualReplacements = applyManualTextReplacementsUseCase(
                            text = output.sanitizedText,
                            commands = activeCommands
                        )
                        var redoText = appliedManualReplacements.text
                        val rebasedRedo = mutableListOf<AppliedManualTextReplacementCommand>()

                        pendingRedoCommands.forEach { command ->
                            val redoResult = applyManualTextReplacementsUseCase(
                                text = redoText,
                                commands = listOf(command)
                            )
                            redoResult.appliedCommands.singleOrNull()?.let { appliedCommand ->
                                redoText = redoResult.text
                                rebasedRedo.add(appliedCommand)
                            }
                        }

                        appliedManualReplacements to rebasedRedo.asReversed()
                    }
                currentCoroutineContext().ensureActive()

                undoHistory.clear()
                undoHistory.addAll(replacementResult.appliedCommands)
                redoHistory.clear()
                redoHistory.addAll(rebasedRedoHistory)
                updateHistoryAvailability()

                _uiState.update {
                    it.copy(
                        sanitizedText = replacementResult.text,
                        entityCounts = output.entityCounts,
                        removedNoiseCount = output.removedNoiseCount,
                        isProcessing = false,
                        errorMessage = null
                    )
                }

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: IllegalArgumentException) {
                currentCoroutineContext().ensureActive()

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível validar o conteúdo detectado. Revise o texto e tente novamente."
                    )
                }

            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível concluir a anonimização. Tente novamente."
                    )
                }
            }
        }
    }

    private fun mapExtractionError(
        exception: DocumentExtractionException
    ): String = when (exception) {
        is DocumentExtractionException.UnsupportedFormat ->
            "Formato de arquivo não suportado. Use TXT, PDF ou ZIP."

        is DocumentExtractionException.FileAccessFailure ->
            "Não foi possível abrir o arquivo selecionado."

        is DocumentExtractionException.InvalidFileSize ->
            "O tamanho do arquivo não pôde ser confirmado. Selecione-o novamente."

        is DocumentExtractionException.FileTooLarge ->
            "O arquivo excede o limite de 20 MB."

        is DocumentExtractionException.ExtractedTextTooLarge ->
            "O texto extraído excede o limite de 10 MB."

        is DocumentExtractionException.PdfTextTooLarge ->
            "O PDF produziu texto além do limite de 10 MB."

        is DocumentExtractionException.ZipEntriesLimitExceeded ->
            "O ZIP contém mais entradas do que o limite suportado."

        is DocumentExtractionException.ZipContentTooLarge ->
            "O conteúdo descompactado do ZIP excede o limite permitido."

        is DocumentExtractionException.ZipTextFileNotFound ->
            "Nenhum arquivo TXT foi encontrado dentro do ZIP."
    }

    // -------------------------------------------------
    // Limpeza
    // -------------------------------------------------

    fun clearSession() {
        processingJob?.cancel()
        extractionJob?.cancel()

        processingJob = null
        extractionJob = null
        undoHistory.clear()
        redoHistory.clear()

        _uiState.value = AnonymizerUiState()
    }

    override fun onCleared() {
        processingJob?.cancel()
        extractionJob?.cancel()

        processingJob = null
        extractionJob = null
        undoHistory.clear()
        redoHistory.clear()

        super.onCleared()
    }

    private fun applyManualReplacement(
        command: ManualTextReplacementCommand,
        notFoundMessage: String
    ) {
        if (_uiState.value.isProcessing) return

        val sourceText = _uiState.value.sanitizedText
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        processingJob = viewModelScope.launch {
            try {
                val result = withContext(computationDispatcher) {
                    applyManualTextReplacementsUseCase(sourceText, listOf(command))
                }
                currentCoroutineContext().ensureActive()

                val appliedCommand = result.appliedCommands.singleOrNull()
                if (appliedCommand == null) {
                    _uiState.update {
                        it.copy(isProcessing = false, errorMessage = notFoundMessage)
                    }
                    return@launch
                }

                undoHistory.add(appliedCommand)
                redoHistory.clear()
                updateHistoryAvailability()
                _uiState.update {
                    it.copy(
                        sanitizedText = result.text,
                        isProcessing = false,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível aplicar a substituição. Tente novamente."
                    )
                }
            }
        }
    }

    private fun updateHistoryAvailability() {
        _uiState.update {
            it.copy(
                manuallyRemovedCount = undoHistory.sumOf { command ->
                    command.changes.count { change ->
                        change.originalText != change.replacementText
                    }
                },
                canUndoManualReplacement = undoHistory.isNotEmpty(),
                canRedoManualReplacement = redoHistory.isNotEmpty()
            )
        }
    }

}
