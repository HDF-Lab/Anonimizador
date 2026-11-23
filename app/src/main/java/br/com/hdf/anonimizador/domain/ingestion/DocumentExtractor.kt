package br.com.hdf.anonimizador.domain.ingestion

import br.com.hdf.anonimizador.domain.model.DocumentSource

interface DocumentExtractor {
    suspend fun extractText(source: DocumentSource): String
}
