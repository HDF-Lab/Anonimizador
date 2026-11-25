package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class PhysicalAddressRegexPiiDetector @Inject constructor() : PiiDetector {

    override val priority: Int = 90

    override suspend fun detect(
        text: String,
        enabledTypes: Set<PiiType>
    ): List<PiiMatch> {
        if (text.isBlank() || PiiType.ENDERECO_FISICO !in enabledTypes) {
            return emptyList()
        }

        val context = currentCoroutineContext()
        val matches = mutableListOf<PiiMatch>()

        for (match in ADDRESS_FIELD_REGEX.findAll(text)) {
            context.ensureActive()

            val value = match.groups[1] ?: continue
            var endIndex = value.range.last + 1
            while (endIndex > value.range.first && text[endIndex - 1] in TRAILING_PUNCTUATION) {
                endIndex--
            }

            if (endIndex <= value.range.first) continue

            matches += PiiMatch(
                startIndex = value.range.first,
                endIndex = endIndex,
                originalValue = text.substring(value.range.first, endIndex),
                type = PiiType.ENDERECO_FISICO,
                confidence = 0.85f,
                detectorPriority = priority
            )
        }

        return matches
    }

    private companion object {
        val ADDRESS_FIELD_REGEX = Regex(
            """(?im)\b(?:endereço|endereco|rua|avenida|residência|residencia)\s*[:=]\s*((?:rua|r\.|avenida|av\.|travessa|tv\.|alameda|al\.|rodovia|rod\.|estrada|estr\.|praça|praca|pça\.|largo|viela|via)\s+[^\r\n;]{3,160})"""
        )
        const val TRAILING_PUNCTUATION = ".,"
    }
}
