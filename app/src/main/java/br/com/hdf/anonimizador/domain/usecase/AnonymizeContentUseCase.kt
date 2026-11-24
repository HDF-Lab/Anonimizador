
package br.com.hdf.anonimizador.domain.usecase

import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.detector.SpanConsolidator
import br.com.hdf.anonimizador.domain.masking.TextMasker
import br.com.hdf.anonimizador.domain.model.AnonymizationOutput
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.parser.WhatsAppParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

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

        val context = currentCoroutineContext()

        context.ensureActive()

        val prepared = prepareText(request)

        context.ensureActive()

        val detectedMatches = detectInParallel(
            text = prepared.text,
            enabledTypes = request.enabledTypes
        )

        context.ensureActive()

        val allMatches = detectedMatches + prepared.participantMatches

        val consolidatedMatches = spanConsolidator.consolidate(
            text = prepared.text,
            matches = allMatches
        )

        context.ensureActive()

        val output = textMasker.mask(
            text = prepared.text,
            matches = consolidatedMatches,
            mode = request.mode
        )

        context.ensureActive()

        return output.copy(
            removedSystemLinesCount = prepared.removedLines
        )
    }

    private suspend fun prepareText(
        request: AnonymizeRequest
    ): PreparedText {

        val context = currentCoroutineContext()

        context.ensureActive()

        val shouldParseWhatsApp =
            whatsAppParser.canParse(request.rawText) &&
                    (request.cleanWhatsAppNoise ||
                            PiiType.PARTICIPANTE_WHATSAPP in request.enabledTypes)

        if (!shouldParseWhatsApp) {
            return PreparedText(
                text = request.rawText,
                participantMatches = emptyList(),
                removedLines = 0
            )
        }

        val parsed = whatsAppParser.parseAndClean(
            rawText = request.rawText,
            cleanNoise = request.cleanWhatsAppNoise
        )

        context.ensureActive()

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
    ): List<PiiMatch> = coroutineScope {

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
        val participantMatches: List<PiiMatch>,
        val removedLines: Int
    )
}
