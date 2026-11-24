package br.com.hdf.anonimizador.domain.model

data class AnonymizeRequest(
    val rawText: String,
    val enabledTypes: Set<PiiType> = PiiType.entries.toSet(),
    val mode: MaskMode = MaskMode.IRREVERSIBLE,
    val noiseCleanupPlatforms: Set<ChatPlatform> = setOf(ChatPlatform.WHATSAPP)
)