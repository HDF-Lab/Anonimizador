
package br.com.hdf.anonimizador.domain.detector

import br.com.hdf.anonimizador.domain.model.PiiMatch

/**
 * Resolve conflitos entre intervalos encontrados pelos detectores.
 *
 * Estratégia:
 * 1. Descarta matches inválidos para o texto analisado.
 * 2. Prioriza os intervalos maiores.
 * 3. Em caso de empate, considera confiança e prioridade do detector.
 * 4. Descarta intervalos que se sobrepõem aos já selecionados.
 * 5. Preserva intervalos adjacentes.
 *
 * Os índices seguem o padrão:
 * [startIndex, endIndex)
 */
interface SpanConsolidator {

    fun consolidate(
        text: String,
        matches: List<PiiMatch>
    ): List<PiiMatch>
}

class DefaultSpanConsolidator : SpanConsolidator {

    override fun consolidate(
        text: String,
        matches: List<PiiMatch>
    ): List<PiiMatch> {

        if (text.isEmpty() || matches.isEmpty()) {
            return emptyList()
        }

        val candidates = matches
            .filter { it.isValidFor(text) }
            .sortedWith(
                compareByDescending<PiiMatch> { it.length }
                    .thenByDescending { it.confidence }
                    .thenByDescending { it.detectorPriority }
                    .thenBy { it.type.ordinal }
                    .thenBy { it.startIndex }
                    .thenBy { it.endIndex }
            )

        val accepted = mutableListOf<PiiMatch>()

        for (candidate in candidates) {

            val hasOverlap = accepted.any { acceptedMatch ->
                overlaps(candidate, acceptedMatch)
            }

            if (!hasOverlap) {
                accepted.add(candidate)
            }
        }

        return accepted.sortedWith(
            compareBy<PiiMatch> { it.startIndex }
                .thenBy { it.endIndex }
        )
    }

    /**
     * Verifica se dois intervalos possuem interseção.
     *
     * Intervalos adjacentes não são considerados sobrepostos.
     */
    private fun overlaps(
        first: PiiMatch,
        second: PiiMatch
    ): Boolean {

        return first.startIndex < second.endIndex &&
                second.startIndex < first.endIndex
    }
}
