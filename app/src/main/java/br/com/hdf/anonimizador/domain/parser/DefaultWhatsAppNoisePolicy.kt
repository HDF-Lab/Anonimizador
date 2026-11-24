package br.com.hdf.anonimizador.domain.parser

import java.util.Locale

internal class DefaultWhatsAppNoisePolicy : NoisePolicy {

    companion object {

        private val EXACT_SYSTEM_MARKERS = setOf(
            "<mídia oculta>",
            "<media omitted>",
            "imagem ocultada",
            "figurinha omitida",
            "áudio ocultado",
            "vídeo omitido",
            "documento omitido",
            "gif omitido",
            "mensagem apagada",
            "esta mensagem foi apagada",
            "você apagou esta mensagem",
            "you deleted this message",
            "this message was deleted"
        )
        private val SYSTEM_NOTIFICATION_PREFIXES = listOf(
            "as mensagens e ligações são protegidas com a criptografia de ponta a ponta",
            "as mensagens e as chamadas são protegidas com a criptografia de ponta a ponta",
            "messages and calls are end-to-end encrypted",
            "you use a default timer for disappearing messages in new chats"
        )

        private val SYSTEM_EVENT_PATTERNS = listOf(
            Regex("""^.+ criou o grupo\.?$"""),
            Regex("""^.+ created the group\.?$"""),

            Regex("""^.+ adicionou .+\.?$"""),
            Regex("""^.+ added .+\.?$"""),

            Regex("""^.+ removeu .+\.?$"""),
            Regex("""^.+ removed .+\.?$"""),

            Regex("""^.+ saiu(?: do grupo)?\.?$"""),
            Regex("""^.+ left(?: the group)?\.?$"""),

            Regex("""^.+ entrou usando o link de convite\.?$"""),
            Regex("""^.+ joined using this group's invite link\.?$"""),

            Regex("""^.+ mudou o nome do grupo(?: de .+ para .+)?\.?$"""),
            Regex("""^.+ changed the group name(?: from .+ to .+)?\.?$"""),

            Regex("""^.+ mudou a imagem (?:deste|do) grupo\.?$"""),
            Regex("""^.+ changed (?:this group's|the group's) (?:icon|image)\.?$"""),

            Regex("""^.+ mudou a descrição (?:deste|do) grupo\.?$"""),
            Regex("""^.+ changed the group description\.?$"""),

            Regex("""^.+ mudou seu código de segurança com .+\.?$"""),
            Regex("""^.+ changed their security code with .+\.?$"""),

            Regex("""^.+ mudou de número de telefone(?: para .+)?\.?$"""),
            Regex("""^.+ changed their phone number\.?$""")
        )

        private val SENDER_INDEPENDENT_SYSTEM_EVENT_PATTERNS = listOf(
            Regex("""^.+ desativou as mensagens temporárias(?:\. mudar duração)?\.?$"""),
            Regex("""^.+ turned off disappearing messages(?:\. change duration)?\.?$""")
        )

        private val INVISIBLE_CHARACTERS = setOf(
            '\u200E',
            '\u200F',
            '\u202A',
            '\u202B',
            '\u202C',
            '\u202D',
            '\u202E',
            '\u2066',
            '\u2067',
            '\u2068',
            '\u2069',
            '\uFEFF'
        )

        private val WHITESPACE_REGEX = Regex("""\s+""")
    }

    override fun isNoise(entry: RawChatEntry): Boolean {
        val body = normalize(entry.body)

        if (body.isEmpty()) return true

        if (body in EXACT_SYSTEM_MARKERS) return true

        if (SYSTEM_NOTIFICATION_PREFIXES.any(body::startsWith)) {
            return true
        }

        if (SENDER_INDEPENDENT_SYSTEM_EVENT_PATTERNS.any { it.matches(body) }) {
            return true
        }

        if (entry.sender == null && isSystemEvent(body)) {
            return true
        }

        return false
    }

    override fun sanitizeInvisibleChars(text: String): String =
        buildString(text.length) {
            text.forEach { char ->
                if (char !in INVISIBLE_CHARACTERS) {
                    append(char)
                }
            }
        }

    private fun normalize(text: String): String =
        sanitizeInvisibleChars(text)
            .trim()
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE_REGEX, " ")

    private fun isSystemEvent(body: String): Boolean =
        SYSTEM_EVENT_PATTERNS.any { it.matches(body) }
}
