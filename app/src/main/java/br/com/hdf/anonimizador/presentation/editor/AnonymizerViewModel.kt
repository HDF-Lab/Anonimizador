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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class AnonymizerViewModel @Inject constructor(
    private val anonymizeContentUseCase: AnonymizeContentUseCase,
    private val documentExtractor: DocumentExtractor,
    @DefaultDispatcher private val computationDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnonymizerUiState())
    val uiState: StateFlow<AnonymizerUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null

    fun onInputTextChanged(extractedText: String) {
        _uiState.update { it.copy(inputText = extractedText, errorMessage = null) }
        scheduleAnonymization(debounceMs = 0L)
    }

    fun onTogglePiiType(type: PiiType) {
        _uiState.update { current ->
            val updated = if (type in current.enabledTypes) {
                current.enabledTypes - type
            } else {
                current.enabledTypes + type
            }
            current.copy(enabledTypes = updated)
        }
        scheduleAnonymization(debounceMs = 0L)
    }

    fun onMaskingModeChanged(mode: MaskMode) {
        _uiState.update { it.copy(maskingMode = mode) }
        scheduleAnonymization(debounceMs = 0L)
    }

    fun onToggleWhatsAppNoise(enabled: Boolean) {
        _uiState.update { it.copy(cleanWhatsAppNoise = enabled) }
        scheduleAnonymization(debounceMs = 0L)
    }

    fun onFileSelected(source: DocumentSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            runCatching {
                documentExtractor.extractText(source)
            }.onSuccess { extractedText ->
                onInputTextChanged(extractedText)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Não foi possível ler o arquivo selecionado."
                    )
                }
            }
        }
    }

    fun clearSession() {
        processingJob?.cancel()
        _uiState.value = AnonymizerUiState()
    }

    private fun scheduleAnonymization(debounceMs: Long) {
        processingJob?.cancel()
        val snapshot = _uiState.value

        if (snapshot.inputText.isBlank()) {
            _uiState.update {
                it.copy(
                    sanitizedText = "",
                    entityCounts = emptyMap(),
                    removedSystemLinesCount = 0,
                    isProcessing = false
                )
            }
            return
        }

        processingJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs.milliseconds)
            _uiState.update { it.copy(isProcessing = true) }

            val output = withContext(computationDispatcher) {
                anonymizeContentUseCase(
                    AnonymizeRequest(
                        rawText = snapshot.inputText,
                        enabledTypes = snapshot.enabledTypes,
                        mode = snapshot.maskingMode,
                        cleanWhatsAppNoise = snapshot.cleanWhatsAppNoise
                    )
                )
            }

            _uiState.update {
                it.copy(
                    sanitizedText = output.sanitizedText,
                    entityCounts = output.entityCounts,
                    removedSystemLinesCount = output.removedSystemLinesCount,
                    isProcessing = false
                )
            }
        }
    }

    override fun onCleared() {
        clearSession()
        super.onCleared()
    }
}
