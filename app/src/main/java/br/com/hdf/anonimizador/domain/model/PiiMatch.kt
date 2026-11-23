
package br.com.hdf.anonimizador.domain.model

/**
 * Representa uma ocorrência de informação sensível localizada no texto.
 *
 * Os índices utilizam o padrão:
 * - startIndex: inclusivo.
 * - endIndex: exclusivo.
 *
 * Exemplo:
 * Texto: "CPF: 12345678900"
 * startIndex = 5
 * endIndex = 16
 */
data class PiiMatch(
    val startIndex: Int,
    val endIndex: Int,
    val originalValue: String,
    val type: PiiType,
    val confidence: Float = 1.0f,
    val detectorPriority: Int = 0
) {

    /**
     * Quantidade de caracteres abrangidos pelo intervalo.
     */
    val length: Int
        get() = endIndex - startIndex

    /**
     * Verifica se o intervalo possui limites válidos.
     */
    fun isValidFor(text: String): Boolean {
        return startIndex >= 0 &&
                endIndex <= text.length &&
                startIndex < endIndex &&
                originalValue.length == length &&
                text.regionMatches(
                    thisOffset = startIndex,
                    other = originalValue,
                    otherOffset = 0,
                    length = originalValue.length
                )
    }
}
