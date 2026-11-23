
package br.com.hdf.anonimizador.domain.usecase

import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.detector.SpanConsolidator
import br.com.hdf.anonimizador.domain.model.AnonymizationOutput
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.parser.WhatsAppParser
import br.com.hdf.anonimizador.domain.masking.TextMasker
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class AnonymizeContentUseCase(
    private val whatsAppParser: WhatsAppParser,
    private val detectors: Set<PiiDetector>,
    private val spanConsolidator: SpanConsolidator,
    private val textMasker: TextMasker
) {

    suspend operator fun invoke(
        request: AnonymizeRequest
    ): AnonymizationOutput {

        if (request.rawText.isBlank()) {
            return emptyOutput()
        }

        val prepared = prepareText(request)

        // Detectores convencionais: CPF, e-mail, telefone etc.
        val detectedMatches = detectInParallel(
            text = prepared.text,
            enabledTypes = request.enabledTypes
        )

        // Combina os matches dos detectores com os participantes do WhatsApp.
        val allMatches = detectedMatches + prepared.participantMatches

        // Resolve eventuais sobreposições.
        val consolidatedMatches = spanConsolidator.consolidate(allMatches)

        // Aplica a anonimização e constrói o vault reversível.
        return textMasker.mask(
            text = prepared.text,
            matches = consolidatedMatches,
            mode = request.mode
        ).copy(
            removedSystemLinesCount = prepared.removedLines
        )
    }

    private suspend fun prepareText(
        request: AnonymizeRequest
    ): PreparedText {

        val shouldParseWhatsApp =
            request.cleanWhatsAppNoise &&
                    whatsAppParser.canParse(request.rawText)

        if (!shouldParseWhatsApp) {
            return PreparedText(
                text = request.rawText,
                participantMatches = emptyList(),
                removedLines = 0
            )
        }

        val parsed = whatsAppParser.parseAndClean(request.rawText)

        // O parser sempre preserva os nomes.
        // A configuração decide se eles serão anonimizados.
        val participantMatches =
            if (PiiType.PARTICIPANTE_WHATSAPP in request.enabledTypes) {
                parsed.participantMatches
            } else {
                emptyList()
            }

        return PreparedText(
            text = parsed.normalizedText,
            participantMatches = participantMatches,
            removedLines = parsed.removedSystemLinesCount
        )
    }

    private suspend fun detectInParallel(
        text: String,
        enabledTypes: Set<PiiType>
    ) = coroutineScope {

        if (enabledTypes.isEmpty()) {
            return@coroutineScope emptyList()
        }

        detectors
            .map { detector ->
                async {
                    detector.detect(text, enabledTypes)
                }
            }
            .awaitAll()
            .flatten()
    }

    private fun emptyOutput() = AnonymizationOutput(
        sanitizedText = "",
        entityCounts = emptyMap()
    )

    private data class PreparedText(
        val text: String,
        val participantMatches: List<br.com.hdf.anonimizador.domain.model.PiiMatch>,
        val removedLines: Int
    )
}
