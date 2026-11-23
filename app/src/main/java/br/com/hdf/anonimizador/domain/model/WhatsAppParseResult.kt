
package br.com.hdf.anonimizador.domain.model

data class WhatsAppParseResult(
    val isWhatsAppFormat: Boolean,
    val normalizedText: String,
    val removedSystemLinesCount: Int,
    val participantMatches: List<PiiMatch> = emptyList()
) {
    /**
     * Quantidade de participantes distintos identificados.
     */
    val participantsCount: Int
        get() = participantMatches
            .map { it.originalValue }
            .distinct()
            .size
}
