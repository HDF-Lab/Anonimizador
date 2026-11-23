package br.com.hdf.anonimizador.domain.masking

import br.com.hdf.anonimizador.domain.model.AnonymizationOutput
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiMatch

/**
 * Contrato 3: Consolida intervalos sobrepostos e aplica as tags de mascaramento.
 */
interface TextMasker {
    suspend fun mask(
        text: String,
        matches: List<PiiMatch>,
        mode: MaskMode
    ): AnonymizationOutput
}