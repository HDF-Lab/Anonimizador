
package br.com.hdf.anonimizador.data.masking

import br.com.hdf.anonimizador.domain.masking.TextMasker
import br.com.hdf.anonimizador.domain.model.AnonymizationOutput
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class SafeTextMasker @Inject constructor() : TextMasker {

    override suspend fun mask(
        text: String,
        matches: List<PiiMatch>,
        mode: MaskMode
    ): AnonymizationOutput {

        if (matches.isEmpty()) {
            return AnonymizationOutput(
                sanitizedText = text,
                entityCounts = emptyMap()
            )
        }

        val context = currentCoroutineContext()

        context.ensureActive()

        val orderedMatches = matches.sortedWith(
            compareBy<PiiMatch> { it.startIndex }
                .thenByDescending { it.endIndex }
                .thenBy { it.type.ordinal }
        )

        validateMatches(
            text = text,
            matches = orderedMatches
        )

        val countersByType = mutableMapOf<PiiType, Int>()
        val tagByOriginalValue = mutableMapOf<Pair<PiiType, String>, String>()
        val counts = mutableMapOf<PiiType, Int>()
        val resolvedMatches = mutableListOf<ResolvedMatch>()

        for (match in orderedMatches) {

            context.ensureActive()

            val originalValue = match.originalValue
            val key = match.type to originalValue

            counts[match.type] =
                (counts[match.type] ?: 0) + 1

            val tag = tagByOriginalValue.getOrPut(key) {
                val nextIndex =
                    (countersByType[match.type] ?: 0) + 1

                countersByType[match.type] = nextIndex

                "[${match.type.name}_$nextIndex]"
            }

            resolvedMatches.add(
                ResolvedMatch(
                    match = match,
                    tag = tag
                )
            )
        }

        context.ensureActive()

        val sanitizedText = replaceMatches(
            text = text,
            resolvedMatches = resolvedMatches
        )

        context.ensureActive()

        val reversalVault = buildReversalVault(
            tagByOriginalValue = tagByOriginalValue,
            mode = mode
        )

        return AnonymizationOutput(
            sanitizedText = sanitizedText,
            entityCounts = counts.toMap(),
            reversalVault = reversalVault
        )
    }

    private suspend fun replaceMatches(
        text: String,
        resolvedMatches: List<ResolvedMatch>
    ): String {

        val context = currentCoroutineContext()
        val builder = StringBuilder(text)

        for ((match, tag) in resolvedMatches.asReversed()) {

            context.ensureActive()

            builder.replace(
                match.startIndex,
                match.endIndex,
                tag
            )
        }

        return builder.toString()
    }

    private fun buildReversalVault(
        tagByOriginalValue: Map<Pair<PiiType, String>, String>,
        mode: MaskMode
    ): Map<String, String> {

        if (mode != MaskMode.REVERSIBLE) {
            return emptyMap()
        }

        return tagByOriginalValue.entries.associate { (key, tag) ->
            tag to key.second
        }
    }

    private fun validateMatches(
        text: String,
        matches: List<PiiMatch>
    ) {

        var maximumEndIndex = -1
        var previousMatch: PiiMatch? = null

        for (match in matches) {

            require(match.isValidFor(text)) {
                "Intervalo ou conteúdo inválido para ${match.type}: " +
                        "${match.startIndex}..${match.endIndex}"
            }

            val previous = previousMatch

            require(match.startIndex >= maximumEndIndex) {
                "Foram encontrados intervalos sobrepostos: " +
                        "${previous?.type} e ${match.type}."
            }

            if (match.endIndex > maximumEndIndex) {
                maximumEndIndex = match.endIndex
                previousMatch = match
            }
        }
    }

    private data class ResolvedMatch(
        val match: PiiMatch,
        val tag: String
    )
}
