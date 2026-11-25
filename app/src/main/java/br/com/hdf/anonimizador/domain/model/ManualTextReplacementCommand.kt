package br.com.hdf.anonimizador.domain.model

sealed interface ManualTextReplacementCommand {
    val target: String
    val replacement: String

    fun execute(text: String): AppliedManualTextReplacementCommand?

    data class ReplaceOccurrence(
        override val target: String,
        override val replacement: String,
        val occurrenceIndex: Int
    ) : ManualTextReplacementCommand {
        override fun execute(text: String): AppliedManualTextReplacementCommand? {
            require(target.isNotEmpty()) { "The replacement target must not be empty." }
            require(occurrenceIndex >= 0) { "The occurrence index must not be negative." }

            val startIndex = text.findOccurrence(target, occurrenceIndex) ?: return null
            return createAppliedCommand(
                text = text,
                command = this,
                startIndices = listOf(startIndex)
            )
        }
    }

    data class ReplaceAll(
        override val target: String,
        override val replacement: String
    ) : ManualTextReplacementCommand {
        override fun execute(text: String): AppliedManualTextReplacementCommand? {
            require(target.isNotEmpty()) { "The replacement target must not be empty." }

            val startIndices = text.findAllOccurrences(target)
            if (startIndices.isEmpty()) return null

            return createAppliedCommand(
                text = text,
                command = this,
                startIndices = startIndices
            )
        }
    }
}

data class AppliedManualTextReplacementCommand(
    val command: ManualTextReplacementCommand,
    val changes: List<TextReplacementChange>,
    val resultText: String
) {
    fun undo(currentText: String): String? {
        var restoredText = currentText

        changes.asReversed().forEach { change ->
            val replacementEnd = change.resultStartIndex + change.replacementText.length
            if (
                change.resultStartIndex < 0 ||
                replacementEnd > restoredText.length ||
                restoredText.substring(change.resultStartIndex, replacementEnd) !=
                change.replacementText
            ) {
                return null
            }

            restoredText = restoredText.replaceRange(
                change.resultStartIndex,
                replacementEnd,
                change.originalText
            )
        }

        return restoredText
    }
}

data class TextReplacementChange(
    val startIndex: Int,
    val resultStartIndex: Int,
    val originalText: String,
    val replacementText: String
)

data class ManualTextReplacementResult(
    val text: String,
    val appliedCommands: List<AppliedManualTextReplacementCommand>
)

private fun createAppliedCommand(
    text: String,
    command: ManualTextReplacementCommand,
    startIndices: List<Int>
): AppliedManualTextReplacementCommand {
    var cumulativeOffset = 0
    val changes = startIndices.map { startIndex ->
        TextReplacementChange(
            startIndex = startIndex,
            resultStartIndex = startIndex + cumulativeOffset,
            originalText = command.target,
            replacementText = command.replacement
        ).also {
            cumulativeOffset += command.replacement.length - command.target.length
        }
    }
    val resultText = changes.asReversed().fold(text) { currentText, change ->
        currentText.replaceRange(
            change.startIndex,
            change.startIndex + change.originalText.length,
            change.replacementText
        )
    }

    return AppliedManualTextReplacementCommand(
        command = command,
        changes = changes,
        resultText = resultText
    )
}

private fun String.findOccurrence(
    target: String,
    occurrenceIndex: Int
): Int? {
    var searchFrom = 0
    repeat(occurrenceIndex + 1) {
        val matchIndex = indexOf(target, searchFrom)
        if (matchIndex < 0) return null
        searchFrom = matchIndex + target.length
        if (it == occurrenceIndex) return matchIndex
    }
    return null
}

private fun String.findAllOccurrences(target: String): List<Int> {
    val startIndices = mutableListOf<Int>()
    var searchFrom = 0

    while (true) {
        val matchIndex = indexOf(target, searchFrom)
        if (matchIndex < 0) return startIndices
        startIndices.add(matchIndex)
        searchFrom = matchIndex + target.length
    }
}
