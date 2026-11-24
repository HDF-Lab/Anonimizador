package br.com.hdf.anonimizador.domain.parser

import br.com.hdf.anonimizador.domain.model.ChatParseResult
import br.com.hdf.anonimizador.domain.model.ChatPlatform
import br.com.hdf.anonimizador.domain.model.PiiType

/**
 * Recognizes and parses an exported conversation independently of its source application.
 */
interface ChatExportParser {

    val platform: ChatPlatform

    val participantPiiType: PiiType?

    fun canParse(rawText: String): Boolean

    /**
     * Parses a recognized export and optionally removes application-specific noise.
     */
    suspend fun parse(
        rawText: String,
        cleanNoise: Boolean = true
    ): ChatParseResult
}
