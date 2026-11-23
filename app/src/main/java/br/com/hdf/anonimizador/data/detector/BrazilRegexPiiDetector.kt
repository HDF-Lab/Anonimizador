
package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.core.util.BrazilianDocumentValidator
import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class BrazilRegexPiiDetector @Inject constructor() : PiiDetector {

    override val priority: Int = 100

    companion object {

        private val CPF_REGEX =
            Regex("""\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b""")

        private val CNPJ_REGEX =
            Regex("""\b\d{2}\.?\d{3}\.?\d{3}/?\d{4}-?\d{2}\b""")

        private val EMAIL_REGEX =
            Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")

        private val PIX_UUID_REGEX =
            Regex(
                """\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""
            )

        private val PHONE_BR_REGEX =
            Regex(
                """(?<!\d)(?:\+55\s?)?\(?\d{2}\)?\s?(?:9\s?)?\d{4}[-\s]?\d{4}(?!\d)"""
            )

        private val CREDIT_CARD_REGEX =
            Regex("""\b(?:\d{4}[-\s]?){3}\d{4}\b""")

        private val LICENSE_PLATE_REGEX =
            Regex(
                """\b[A-Z]{3}-?\d[A-Z0-9]\d{2}\b""",
                RegexOption.IGNORE_CASE
            )

        private val IPV4_REGEX =
            Regex(
                """\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\b"""
            )
    }

    override suspend fun detect(
        text: String,
        enabledTypes: Set<PiiType>
    ): List<PiiMatch> {

        if (text.isBlank() || enabledTypes.isEmpty()) {
            return emptyList()
        }

        val context = currentCoroutineContext()
        val matches = mutableListOf<PiiMatch>()

        context.ensureActive()

        if (PiiType.CHAVE_PIX in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = PIX_UUID_REGEX,
                type = PiiType.CHAVE_PIX
            )
        }

        context.ensureActive()

        if (PiiType.EMAIL in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = EMAIL_REGEX,
                type = PiiType.EMAIL
            )
        }

        context.ensureActive()

        if (PiiType.CPF in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = CPF_REGEX,
                type = PiiType.CPF,
                validator = BrazilianDocumentValidator::isValidCpf
            )
        }

        context.ensureActive()

        if (PiiType.CNPJ in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = CNPJ_REGEX,
                type = PiiType.CNPJ,
                validator = BrazilianDocumentValidator::isValidCnpj
            )
        }

        context.ensureActive()

        if (PiiType.CARTAO_CREDITO in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = CREDIT_CARD_REGEX,
                type = PiiType.CARTAO_CREDITO,
                validator = BrazilianDocumentValidator::isValidLuhn
            )
        }

        context.ensureActive()

        if (PiiType.TELEFONE_BR in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = PHONE_BR_REGEX,
                type = PiiType.TELEFONE_BR
            )
        }

        context.ensureActive()

        if (PiiType.PLACA_VEICULO in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = LICENSE_PLATE_REGEX,
                type = PiiType.PLACA_VEICULO
            )
        }

        context.ensureActive()

        if (PiiType.ENDERECO_IP in enabledTypes) {
            matches += findMatches(
                text = text,
                regex = IPV4_REGEX,
                type = PiiType.ENDERECO_IP
            )
        }

        context.ensureActive()

        return matches
    }

    private suspend fun findMatches(
        text: String,
        regex: Regex,
        type: PiiType,
        validator: (String) -> Boolean = { true }
    ): List<PiiMatch> {

        val context = currentCoroutineContext()
        val matches = mutableListOf<PiiMatch>()

        for (match in regex.findAll(text)) {
            context.ensureActive()

            if (!validator(match.value)) {
                continue
            }

            matches += PiiMatch(
                startIndex = match.range.first,
                endIndex = match.range.last + 1,
                originalValue = match.value,
                type = type,
                confidence = 1.0f,
                detectorPriority = priority
            )
        }

        return matches
    }
}
