package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class UrlRegexPiiDetector @Inject constructor() : PiiDetector {

    override val priority: Int = 100

    override suspend fun detect(
        text: String,
        enabledTypes: Set<PiiType>
    ): List<PiiMatch> {
        if (text.isBlank() || PiiType.LINK !in enabledTypes) {
            return emptyList()
        }

        val context = currentCoroutineContext()
        val matches = mutableListOf<PiiMatch>()

        for (match in URL_REGEX.findAll(text)) {
            context.ensureActive()

            val endIndex = trimmedEndIndex(text, match.range.first, match.range.last + 1)

            if (endIndex <= match.range.first) continue

            matches += PiiMatch(
                startIndex = match.range.first,
                endIndex = endIndex,
                originalValue = text.substring(match.range.first, endIndex),
                type = PiiType.LINK,
                confidence = 1.0f,
                detectorPriority = priority
            )
        }

        return matches.sortedBy(PiiMatch::startIndex)
    }

    private companion object {
        val URL_REGEX = Regex(
            """(?i)\b(?:https?://|www\.)[^\s<>"']+|\bgeo:-?\d{1,3}(?:\.\d+)?,-?\d{1,3}(?:\.\d+)?(?:\?[^\s<>"']*)?"""
        )
        val MAP_LOCATION_REGEX = Regex(
            """(?i)(?:maps(?:\.app)?\.google\.[a-z.]+|maps\.app\.goo\.gl|goo\.gl/maps|google\.[a-z.]+/maps|geo:-?\d{1,3}(?:\.\d+)?,-?\d{1,3}(?:\.\d+)?)"""
        )
        const val TRAILING_PUNCTUATION = ".,!?;:)]}"

        fun trimmedEndIndex(text: String, startIndex: Int, initialEndIndex: Int): Int {
            var endIndex = initialEndIndex
            while (endIndex > startIndex && text[endIndex - 1] in TRAILING_PUNCTUATION) {
                endIndex--
            }
            return endIndex
        }
    }
}
