
package br.com.hdf.anonimizador.data.masking

import br.com.hdf.anonimizador.domain.masking.TextMasker
import br.com.hdf.anonimizador.domain.model.AnonymizationOutput
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
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

        val orderedMatches = matches.sortedWith(
            compareBy<PiiMatch> { it.startIndex }
                .thenByDescending { it.endIndex }
                .thenBy { it.type.ordinal }
        )

        validateMatches(text, orderedMatches)

        val countersByType = mutableMapOf<PiiType, Int>()
        val tagByOriginalValue =
            mutableMapOf<Pair<PiiType, String>, String>()

        val counts = mutableMapOf<PiiType, Int>()

        val resolvedTags = orderedMatches.map { match ->

            val originalValue = text.substring(
                match.startIndex,
                match.endIndex
            )

            counts[match.type] =
                (counts[match.type] ?: 0) + 1

            val key = match.type to originalValue

            val tag = tagByOriginalValue.getOrPut(key) {
                val nextIndex =
                    (countersByType[match.type] ?: 0) + 1

                countersByType[match.type] = nextIndex

                "[${match.type.name}_$nextIndex]"
            }

            ResolvedMatch(
                match = match,
                tag = tag
            )
        }

        val sanitizedText = replaceMatches(
            text = text,
            resolvedMatches = resolvedTags
        )

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

    private fun replaceMatches(
        text: String,
        resolvedMatches: List<ResolvedMatch>
    ): String {

        val builder = StringBuilder(text)

        resolvedMatches.asReversed().forEach { resolved ->
            builder.replace(
                resolved.match.startIndex,
                resolved.match.endIndex,
                resolved.tag
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

        matches.forEachIndexed { index, match ->

            require(
                match.startIndex >= 0 &&
                        match.endIndex <= text.length &&
                        match.startIndex < match.endIndex
            ) {
                "Intervalo inválido para ${match.type}: " +
                        "${match.startIndex}..${match.endIndex}"
            }

            val actualValue = text.substring(
                match.startIndex,
                match.endIndex
            )

            require(actualValue == match.originalValue) {
                "O valor original não corresponde ao intervalo " +
                        "para ${match.type}."
            }

            if (index > 0) {
                val previous = matches[index - 1]

                require(previous.endIndex <= match.startIndex) {
                    "Foram encontrados intervalos sobrepostos: " +
                            "${previous.type} e ${match.type}."
                }
            }
        }
    }

    private data class ResolvedMatch(
        val match: PiiMatch,
        val tag: String
    )
}
