package br.com.hdf.anonimizador.domain.model

enum class MaskMode {
    IRREVERSIBLE, // Ex: [CPF_OCULTO] ou [CPF_1]
    REVERSIBLE    // Ex: [CPF_1] com mapa mantido apenas na RAM da sessão
}