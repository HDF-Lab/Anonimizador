
package br.com.hdf.anonimizador.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hdf.anonimizador.di.DefaultDispatcher
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractor
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import br.com.hdf.anonimizador.domain.model.DocumentSource
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiType
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
    private val documentExtractor: DocumentExtractor,
    @DefaultDispatcher private val computationDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnonymizerUiState())

    val uiState: StateFlow<AnonymizerUiState> =
        _uiState.asStateFlow()

    private var processingJob: Job? = null
    private var extractionJob: Job? = null

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

        _uiState.update {
            it.copy(
                inputText = text,
                errorMessage = null
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

            } catch (exception: Exception) {
                currentCoroutineContext().ensureActive()

                extractionJob = null

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível ler o arquivo selecionado."
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
            _uiState.update {
                it.copy(
                    sanitizedText = "",
                    entityCounts = emptyMap(),
                    removedSystemLinesCount = 0,
                    isProcessing = false,
                    errorMessage = null
                )
            }
            return
        }

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
                    cleanWhatsAppNoise = snapshot.cleanWhatsAppNoise
                )

                val output = withContext(computationDispatcher) {
                    anonymizeContentUseCase(request)
                }

                currentCoroutineContext().ensureActive()

                _uiState.update {
                    it.copy(
                        sanitizedText = output.sanitizedText,
                        entityCounts = output.entityCounts,
                        removedSystemLinesCount = output.removedSystemLinesCount,
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
                        errorMessage = "Não foi possível anonimizar o texto."
                    )
                }
            }
        }
    }

    // -------------------------------------------------
    // Limpeza
    // -------------------------------------------------

    fun clearSession() {
        processingJob?.cancel()
        extractionJob?.cancel()

        processingJob = null
        extractionJob = null

        _uiState.value = AnonymizerUiState()
    }

    override fun onCleared() {
        processingJob?.cancel()
        extractionJob?.cancel()

        processingJob = null
        extractionJob = null

        super.onCleared()
    }
}
