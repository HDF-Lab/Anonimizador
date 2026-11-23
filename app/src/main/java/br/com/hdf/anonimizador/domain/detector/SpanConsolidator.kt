
package br.com.hdf.anonimizador.domain.detector

import br.com.hdf.anonimizador.domain.model.PiiMatch

interface SpanConsolidator {
    fun consolidate(matches: List<PiiMatch>): List<PiiMatch>
}

class DefaultSpanConsolidator : SpanConsolidator {

    override fun consolidate(
        matches: List<PiiMatch>
    ): List<PiiMatch> {
        if (matches.isEmpty()) return emptyList()

        val candidates = matches
            .filter(::hasValidInterval)
            .sortedWith(
                compareByDescending<PiiMatch> { intervalLength(it) }
                    .thenByDescending { it.confidence }
                    .thenByDescending { it.detectorPriority }
                    .thenBy { it.type.ordinal }
                    .thenBy { it.startIndex }
                    .thenBy { it.endIndex }
            )

        val accepted = mutableListOf<PiiMatch>()

        for (candidate in candidates) {
            val hasConflict = accepted.any {
                overlaps(it, candidate)
            }

            if (!hasConflict) {
                accepted.add(candidate)
            }
        }

        return accepted.sortedWith(
            compareBy<PiiMatch> { it.startIndex }
                .thenBy { it.endIndex }
        )
    }

    private fun hasValidInterval(match: PiiMatch): Boolean =
        match.startIndex >= 0 &&
                match.endIndex > match.startIndex

    private fun intervalLength(match: PiiMatch): Int =
        match.endIndex - match.startIndex

    private fun overlaps(
        first: PiiMatch,
        second: PiiMatch
    ): Boolean =
        first.startIndex < second.endIndex &&
                second.startIndex < first.endIndex
}
