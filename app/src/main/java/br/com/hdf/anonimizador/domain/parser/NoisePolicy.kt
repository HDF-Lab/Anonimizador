package br.com.hdf.anonimizador.domain.parser

internal interface NoisePolicy {

    fun isNoise(entry: RawChatEntry): Boolean

    fun sanitizeInvisibleChars(text: String): String
}
