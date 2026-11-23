package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.core.util.BrazilianDocumentValidator
import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import javax.inject.Inject

class BrazilRegexPiiDetector @Inject constructor() : PiiDetector {

    override val priority: Int = 100

    companion object {
        private val CPF_REGEX = Regex("""\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b""")
        private val CNPJ_REGEX = Regex("""\b\d{2}\.?\d{3}\.?\d{3}/?\d{4}-?\d{2}\b""")
        private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
        private val PIX_UUID_REGEX = Regex(
                """\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""
        )
        private val PHONE_BR_REGEX = Regex(
                """(?<!\d)(?:\+55\s?)?\(?\d{2}\)?\s?(?:9\s?)?\d{4}[-\s]?\d{4}(?!\d)"""
        )
        private val CREDIT_CARD_REGEX = Regex("""\b(?:\d{4}[-\s]?){3}\d{4}\b""")
        private val LICENSE_PLATE_REGEX = Regex("""\b[A-Z]{3}-?\d[A-Z0-9]\d{2}\b""", RegexOption.IGNORE_CASE)
        private val IPV4_REGEX = Regex(
                """\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\b"""
        )
    }

    override suspend fun detect(text: String, enabledTypes: Set<PiiType>): List<PiiMatch> {
        if (text.isBlank() || enabledTypes.isEmpty()) return emptyList()

        val matches = mutableListOf<PiiMatch>()

        if (PiiType.CHAVE_PIX in enabledTypes) {
            matches += findMatches(text, PIX_UUID_REGEX, PiiType.CHAVE_PIX)
        }
        if (PiiType.EMAIL in enabledTypes) {
            matches += findMatches(text, EMAIL_REGEX, PiiType.EMAIL)
        }
        if (PiiType.CPF in enabledTypes) {
            matches += findMatches(text, CPF_REGEX, PiiType.CPF, BrazilianDocumentValidator::isValidCpf)
        }
        if (PiiType.CNPJ in enabledTypes) {
            matches += findMatches(text, CNPJ_REGEX, PiiType.CNPJ, BrazilianDocumentValidator::isValidCnpj)
        }
        if (PiiType.CARTAO_CREDITO in enabledTypes) {
            matches += findMatches(text, CREDIT_CARD_REGEX, PiiType.CARTAO_CREDITO, BrazilianDocumentValidator::isValidLuhn)
        }
        if (PiiType.TELEFONE_BR in enabledTypes) {
            matches += findMatches(text, PHONE_BR_REGEX, PiiType.TELEFONE_BR)
        }
        if (PiiType.PLACA_VEICULO in enabledTypes) {
            matches += findMatches(text, LICENSE_PLATE_REGEX, PiiType.PLACA_VEICULO)
        }
        if (PiiType.ENDERECO_IP in enabledTypes) {
            matches += findMatches(text, IPV4_REGEX, PiiType.ENDERECO_IP)
        }

        return matches
    }

    private fun findMatches(
            text: String,
            regex: Regex,
            type: PiiType,
            validator: (String) -> Boolean = { true }
    ): List<PiiMatch> =
    regex.findAll(text)
            .filter { match -> validator(match.value) }
            .map { match ->
            PiiMatch(
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1,
                    originalValue = match.value,
                    type = type,
                    confidence = 1.0f
            )
    }
            .toList()

}
