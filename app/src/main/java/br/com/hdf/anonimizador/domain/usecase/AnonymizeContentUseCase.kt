
package br.com.hdf.anonimizador.domain.usecase

import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.detector.SpanConsolidator
import br.com.hdf.anonimizador.domain.masking.TextMasker
import br.com.hdf.anonimizador.domain.model.AnonymizationOutput
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.parser.ChatExportParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class AnonymizeContentUseCase(
    private val chatExportParsers: Set<ChatExportParser>,
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
            removedNoiseCount = prepared.removedNoise
        )
    }

    private suspend fun prepareText(
        request: AnonymizeRequest
    ): PreparedText {

        val context = currentCoroutineContext()

        context.ensureActive()

        val parser = selectParser(request.rawText)
        val shouldCleanNoise =
            parser?.platform?.let(request.noiseCleanupPlatforms::contains) == true
        val participantPiiType = parser?.participantPiiType
        val participantPiiTypeToAnonymize =
            participantPiiType?.takeIf(request.enabledTypes::contains)

        if (parser == null || (!shouldCleanNoise && participantPiiTypeToAnonymize == null)) {
            return PreparedText(
                text = request.rawText,
                participantMatches = emptyList(),
                removedNoise = 0
            )
        }

        val parsed = parser.parse(
            rawText = request.rawText,
            cleanNoise = shouldCleanNoise
        )

        context.ensureActive()

        val participantMatches = participantPiiTypeToAnonymize?.let { piiType ->
            parsed.participantOccurrences.map { occurrence ->
                PiiMatch(
                    startIndex = occurrence.startIndex,
                    endIndex = occurrence.endIndex,
                    originalValue = occurrence.originalValue,
                    type = piiType
                )
            }
        }.orEmpty()

        return PreparedText(
            text = parsed.normalizedText,
            participantMatches = participantMatches,
            removedNoise = parsed.removedNoiseCount
        )
    }

    private fun selectParser(rawText: String): ChatExportParser? {
        val matchingParsers = chatExportParsers.filter { it.canParse(rawText) }

        check(matchingParsers.size <= 1) {
            "Multiple chat export parsers recognized the same input."
        }

        return matchingParsers.singleOrNull()
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
        val removedNoise: Int
    )
}
