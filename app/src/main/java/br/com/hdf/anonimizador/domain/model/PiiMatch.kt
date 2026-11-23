package br.com.hdf.anonimizador.domain.model

/**
 * Representa uma ocorrência sensível localizada no texto (índices inclusivo/exclusivo).
 */
data class PiiMatch(
    val startIndex: Int,
    val endIndex: Int,
    val originalValue: String,
    val type: PiiType,
    val confidence: Float = 1.0f,
    val detectorPriority: Int = 0
)