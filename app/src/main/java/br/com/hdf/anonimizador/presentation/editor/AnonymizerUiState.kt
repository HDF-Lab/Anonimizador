package br.com.hdf.anonimizador.presentation.editor

import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiType

data class AnonymizerUiState (
    val inputText: String = "",
    val sanitizedText: String = "",
    val enabledTypes: Set<PiiType> = PiiType.entries.toSet(),
    val maskingMode: MaskMode = MaskMode.IRREVERSIBLE,
    val cleanWhatsAppNoise: Boolean = true,
    val entityCounts: Map<PiiType, Int> = emptyMap(),
    val removedNoiseCount: Int = 0,
    val manuallyRemovedCount: Int = 0,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val canUndoManualReplacement: Boolean = false,
    val canRedoManualReplacement: Boolean = false
) {
    val totalMaskedCount: Int
        get() = entityCounts.values.sum()

    val hasContent: Boolean
        get() = inputText.isNotBlank()
}
