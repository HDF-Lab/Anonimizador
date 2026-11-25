package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class NameRegexPiiDetector @Inject constructor() : PiiDetector {

    override val priority: Int = 90

    override suspend fun detect(
        text: String,
        enabledTypes: Set<PiiType>
    ): List<PiiMatch> {
        if (text.isBlank() || PiiType.NOME_PROPRIO !in enabledTypes) {
            return emptyList()
        }

        val context = currentCoroutineContext()
        val matches = mutableListOf<PiiMatch>()

        for (match in NAME_FIELD_REGEX.findAll(text)) {
            context.ensureActive()

            val value = match.groups[1] ?: continue
            val startIndex = value.range.first
            val endIndex = value.range.last + 1
            val name = text.substring(startIndex, endIndex).trimEnd('.', ' ')
            val trimmedEndIndex = startIndex + name.length

            if (name.isBlank()) continue

            matches += PiiMatch(
                startIndex = startIndex,
                endIndex = trimmedEndIndex,
                originalValue = name,
                type = PiiType.NOME_PROPRIO,
                confidence = 0.9f,
                detectorPriority = priority
            )
        }

        return matches
    }

    private companion object {
        val NAME_FIELD_REGEX = Regex(
            """(?im)\b(?:nome(?:\s+completo)?|titular)\s*[:=]\s*([\p{L}][\p{L}'’\-]*(?:\s+[\p{L}][\p{L}'’\-]*){0,5})(?=\s*(?:[,;.]|\r?$))"""
        )
    }
}
