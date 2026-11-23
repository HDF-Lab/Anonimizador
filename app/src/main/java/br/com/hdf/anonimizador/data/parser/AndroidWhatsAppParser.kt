
package br.com.hdf.anonimizador.data.parser

import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.model.WhatsAppParseResult
import br.com.hdf.anonimizador.domain.parser.RawChatEntry
import br.com.hdf.anonimizador.domain.parser.WhatsAppNoisePolicy
import br.com.hdf.anonimizador.domain.parser.WhatsAppParser

class AndroidWhatsAppParser internal constructor(
    private val noisePolicy: WhatsAppNoisePolicy
) : WhatsAppParser {

    constructor() : this(WhatsAppNoisePolicy())

    companion object {

        private val MESSAGE_HEADER_REGEX = Regex(
            pattern = """^(\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2})\s+-\s+(?:([^:]+):\s)?(.*)$"""
        )

        private const val SAMPLE_LINES_FOR_DETECTION = 15
    }

    override fun canParse(rawText: String): Boolean {
        return rawText.isNotBlank() && rawText.lineSequence()
            .map { noisePolicy.sanitizeInvisibleChars(it).trim() }
            .filter { it.isNotEmpty() }
            .take(SAMPLE_LINES_FOR_DETECTION)
            .any(::isMessageHeader)
    }

    override suspend fun parseAndClean(
        rawText: String
    ): WhatsAppParseResult {

        if (!canParse(rawText)) {
            return WhatsAppParseResult(
                isWhatsAppFormat = false,
                normalizedText = noisePolicy
                    .sanitizeInvisibleChars(rawText)
                    .trim(),
                removedSystemLinesCount = 0
            )
        }

        val entries = groupMultilineEntries(rawText)

        val normalizedText = StringBuilder()
        val participantMatches = mutableListOf<PiiMatch>()

        var removedCount = 0

        entries.forEach { entry ->

            if (noisePolicy.isNoise(entry)) {
                removedCount++
                return@forEach
            }

            val sender = entry.sender
                ?.trim()
                ?.takeIf(String::isNotEmpty)

            val cleanBody = noisePolicy
                .sanitizeInvisibleChars(entry.body)
                .trim()

            appendLineSeparator(normalizedText)

            normalizedText.append(entry.timestamp)
            normalizedText.append(" - ")

            if (sender != null) {
                appendParticipant(
                    text = normalizedText,
                    matches = participantMatches,
                    sender = sender
                )
            }

            normalizedText.append(": ")
            normalizedText.append(cleanBody)
        }

        return WhatsAppParseResult(
            isWhatsAppFormat = true,
            normalizedText = normalizedText.toString(),
            removedSystemLinesCount = removedCount,
            participantMatches = participantMatches
        )
    }

    private fun isMessageHeader(line: String): Boolean =
        MESSAGE_HEADER_REGEX.matches(line)

    private fun groupMultilineEntries(
        rawText: String
    ): List<RawChatEntry> {

        val entries = mutableListOf<RawChatEntry>()
        var currentEntry: RawChatEntry? = null

        rawText.lineSequence().forEach { rawLine ->

            val cleanLine = noisePolicy
                .sanitizeInvisibleChars(rawLine)

            val headerLine = cleanLine.trimStart()
            val match = MESSAGE_HEADER_REGEX.matchEntire(headerLine)

            if (match != null) {
                currentEntry?.let(entries::add)
                currentEntry = extractEntryFromMatch(match)
            } else {
                currentEntry = currentEntry?.let { entry ->
                    appendContinuationLine(entry, cleanLine)
                }
            }
        }

        currentEntry?.let(entries::add)

        return entries
    }

    private fun extractEntryFromMatch(
        match: MatchResult
    ): RawChatEntry {

        val groups = match.groupValues

        val timestamp = groups[1]

        val sender = groups[2]
            .takeIf(String::isNotBlank)

        val body = groups[3]

        return RawChatEntry(
            timestamp = timestamp,
            sender = sender,
            body = body
        )
    }

    private fun appendContinuationLine(
        entry: RawChatEntry,
        nextLine: String
    ): RawChatEntry =
        entry.copy(
            body = "${entry.body}\n$nextLine"
        )

    private fun appendLineSeparator(
        text: StringBuilder
    ) {
        if (text.isNotEmpty()) {
            text.append('\n')
        }
    }

    private fun appendParticipant(
        text: StringBuilder,
        matches: MutableList<PiiMatch>,
        sender: String
    ) {

        val startIndex = text.length

        text.append(sender)

        val endIndex = text.length

        matches += PiiMatch(
            startIndex = startIndex,
            endIndex = endIndex,
            originalValue = sender,
            type = PiiType.PARTICIPANTE_WHATSAPP
        )
    }
}
