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

            var endIndex = match.range.last + 1
            while (endIndex > match.range.first && text[endIndex - 1] in TRAILING_PUNCTUATION) {
                endIndex--
            }

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

        return matches
    }

    private companion object {
        val URL_REGEX = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"']+""")
        const val TRAILING_PUNCTUATION = ".,!?;:)]}"
    }
}
