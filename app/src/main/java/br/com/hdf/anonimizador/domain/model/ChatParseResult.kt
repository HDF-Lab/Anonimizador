package br.com.hdf.anonimizador.domain.model

data class ChatParseResult(
    val isSupportedFormat: Boolean,
    val normalizedText: String,
    val removedNoiseCount: Int,
    val participantOccurrences: List<ParticipantOccurrence> = emptyList()
) {
    val participantsCount: Int
        get() = participantOccurrences
            .map { it.originalValue }
            .distinct()
            .size
}

data class ParticipantOccurrence(
    val startIndex: Int,
    val endIndex: Int,
    val originalValue: String
)
