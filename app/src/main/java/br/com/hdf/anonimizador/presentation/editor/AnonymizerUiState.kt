package br.com.hdf.anonimizador.presentation.editor

import androidx.annotation.StringRes
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiType

const val DEFAULT_MANUAL_REDACTION_MARKER = "[REMOVIDO]"
const val MAX_MANUAL_REDACTION_MARKER_LENGTH = 40

data class AnonymizerUiState (
    val inputText: String = "",
    val sanitizedText: String = "",
    val enabledTypes: Set<PiiType> = PiiType.entries.toSet(),
    val maskingMode: MaskMode = MaskMode.IRREVERSIBLE,
    val cleanWhatsAppNoise: Boolean = true,
    val manualRedactionMarker: String = DEFAULT_MANUAL_REDACTION_MARKER,
    val entityCounts: Map<PiiType, Int> = emptyMap(),
    val removedNoiseCount: Int = 0,
    val manuallyRemovedCount: Int = 0,
    val isProcessing: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val canUndoManualReplacement: Boolean = false,
    val canRedoManualReplacement: Boolean = false
) {
    val totalMaskedCount: Int
        get() = entityCounts.values.sum()

    val hasContent: Boolean
        get() = inputText.isNotBlank()
}
