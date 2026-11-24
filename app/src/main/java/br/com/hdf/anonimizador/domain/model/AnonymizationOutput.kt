package br.com.hdf.anonimizador.domain.model

data class AnonymizationOutput(
    val sanitizedText: String,
    val entityCounts: Map<PiiType, Int>,
    val reversalVault: Map<String, String> = emptyMap(), // Vazio no modo IRREVERSIVEL
    val removedNoiseCount: Int = 0
)