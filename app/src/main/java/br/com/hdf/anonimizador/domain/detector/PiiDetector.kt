
package br.com.hdf.anonimizador.domain.detector

import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType

/**
 * Contrato para detectores de informações pessoais.
 *
 * Implementações podem utilizar diferentes estratégias, como:
 * - Expressões regulares.
 * - Validações determinísticas.
 * - Modelos NER executados on-device.
 *
 * Responsabilidades:
 * - Identificar somente os tipos habilitados.
 * - Retornar intervalos válidos sobre o texto original recebido.
 * - Informar a prioridade do detector em cada correspondência.
 * - Respeitar o cancelamento da coroutine durante operações demoradas.
 */
interface PiiDetector {

    /**
     * Prioridade utilizada pelo SpanConsolidator
     * quando existem correspondências sobrepostas.
     *
     * Valores maiores representam maior prioridade,
     * desde que essa regra seja mantida pelo consolidador.
     */
    val priority: Int

    /**
     * Detecta informações pessoais no texto.
     *
     * @param text Texto original que será analisado.
     * @param enabledTypes Tipos de informação permitidos na detecção.
     *
     * @return Correspondências encontradas, com índices relativos
     * ao texto recebido.
     */
    suspend fun detect(
        text: String,
        enabledTypes: Set<PiiType>
    ): List<PiiMatch>
}
