package br.com.hdf.anonimizador.domain.detector

import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType

/**
 * Contrato 2: Implementado tanto pela camada Regex quanto pelo modelo NER on-device.
 */
interface PiiDetector {
    val priority: Int // Usado para desempate quando dois detectores marcam o mesmo trecho
    suspend fun detect(text: String, enabledTypes: Set<PiiType>): List<PiiMatch>
}