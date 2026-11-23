package br.com.hdf.anonimizador.domain.parser

/**
 * Representa uma entrada bruta já desmembrada do formato Android:
 * "DD/MM/AAAA HH:MM - Remetente: Mensagem"
 */
internal data class RawChatEntry(
    val timestamp: String,
    val sender: String?,
    val body: String
) {
    val isSystemEventWithoutSender: Boolean
        get() = sender.isNullOrBlank()
}
