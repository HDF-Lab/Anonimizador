
package br.com.hdf.anonimizador.data.parser

import br.com.hdf.anonimizador.domain.model.ChatParseResult
import br.com.hdf.anonimizador.domain.model.ChatPlatform
import br.com.hdf.anonimizador.domain.model.ParticipantOccurrence
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.parser.DefaultWhatsAppNoisePolicy
import br.com.hdf.anonimizador.domain.parser.RawChatEntry
import br.com.hdf.anonimizador.domain.parser.NoisePolicy
import br.com.hdf.anonimizador.domain.parser.ChatExportParser

class AndroidWhatsAppParser internal constructor(
    private val noisePolicy: NoisePolicy
) : ChatExportParser {

    constructor() : this(DefaultWhatsAppNoisePolicy())

    override val platform: ChatPlatform = ChatPlatform.WHATSAPP

    override val participantPiiType: PiiType = PiiType.PARTICIPANTE_WHATSAPP

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

    override suspend fun parse(
        rawText: String,
        cleanNoise: Boolean
    ): ChatParseResult {

        if (!canParse(rawText)) {
            return ChatParseResult(
                isSupportedFormat = false,
                normalizedText = noisePolicy
                    .sanitizeInvisibleChars(rawText)
                    .trim(),
                removedNoiseCount = 0
            )
        }

        val entries = groupMultilineEntries(rawText)

        val normalizedText = StringBuilder()
        val participantNames = linkedSetOf<String>()

        var removedCount = 0

        entries.forEach { entry ->

            if (cleanNoise && noisePolicy.isNoise(entry)) {
                removedCount++
                return@forEach
            }

            val sender = entry.sender
                ?.trim()
                ?.takeIf(String::isNotEmpty)

            sender?.let(participantNames::add)

            if (!cleanNoise) {
                return@forEach
            }

            val cleanBody = noisePolicy
                .sanitizeInvisibleChars(entry.body)
                .trim()

            appendLineSeparator(normalizedText)

            normalizedText.append(entry.timestamp)
            normalizedText.append(" - ")

            if (sender != null) {
                normalizedText.append(sender)
            }

            normalizedText.append(": ")
            normalizedText.append(cleanBody)
        }

        val outputText = if (cleanNoise) normalizedText.toString() else rawText

        return ChatParseResult(
            isSupportedFormat = true,
            normalizedText = outputText,
            removedNoiseCount = removedCount,
            participantOccurrences = findParticipantOccurrences(outputText, participantNames)
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

    private fun findParticipantOccurrences(
        text: String,
        participantNames: Set<String>
    ): List<ParticipantOccurrence> {
        if (participantNames.isEmpty()) return emptyList()

        val namesPattern = participantNames
            .sortedByDescending(String::length)
            .joinToString("|") { Regex.escape(it) }
        val pattern = Regex(
            """(?<![\p{L}\p{N}])(?:$namesPattern)(?![\p{L}\p{N}])""",
            RegexOption.IGNORE_CASE
        )

        return pattern.findAll(text).map { match ->
            ParticipantOccurrence(
                startIndex = match.range.first,
                endIndex = match.range.last + 1,
                originalValue = match.value
            )
        }.toList()
    }
}
