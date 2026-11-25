package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class CnhRegexPiiDetector @Inject constructor() : PiiDetector {

    override val priority: Int = 110

    override suspend fun detect(
        text: String,
        enabledTypes: Set<PiiType>
    ): List<PiiMatch> {
        if (text.isBlank() || PiiType.CNH !in enabledTypes) {
            return emptyList()
        }

        val context = currentCoroutineContext()
        val matches = mutableListOf<PiiMatch>()

        for (match in CNH_REGEX.findAll(text)) {
            context.ensureActive()

            val value = match.groups[1] ?: continue
            if (value.value.count(Char::isDigit) != CNH_DIGIT_COUNT) continue

            matches += PiiMatch(
                startIndex = value.range.first,
                endIndex = value.range.last + 1,
                originalValue = value.value,
                type = PiiType.CNH,
                confidence = 1.0f,
                detectorPriority = priority
            )
        }

        return matches
    }

    private companion object {
        const val CNH_DIGIT_COUNT = 11
        val CNH_REGEX = Regex(
            """(?i)\b(?:CNH|Carteira\s+Nacional\s+de\s+Habilitação|Carteira\s+Nacional\s+de\s+Habilitacao)(?:\s*(?:n[º°.]?\s*)?)\s*[:#=\-]?\s*((?:\d[ .-]?){10}\d)(?!\d)"""
        )
    }
}
