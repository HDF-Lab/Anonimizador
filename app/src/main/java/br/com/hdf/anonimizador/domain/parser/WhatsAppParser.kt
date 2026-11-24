package br.com.hdf.anonimizador.domain.parser

import br.com.hdf.anonimizador.domain.model.WhatsAppParseResult

/**
 * Interpreta conversas exportadas do WhatsApp.
 *
 * Responsabilidades:
 * - Identificar o formato da conversa.
 * - Agrupar mensagens e continuações.
 * - Normalizar a estrutura das mensagens.
 * - Identificar os participantes.
 * - Remover mensagens de sistema quando solicitado.
 *
 * Não realiza anonimização dos participantes.
 */
interface WhatsAppParser {

    fun canParse(rawText: String): Boolean

    suspend fun parseAndClean(
        rawText: String,
        cleanNoise: Boolean = true
    ): WhatsAppParseResult
}