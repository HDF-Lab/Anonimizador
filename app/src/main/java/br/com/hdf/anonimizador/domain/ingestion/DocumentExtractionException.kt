package br.com.hdf.anonimizador.domain.ingestion

sealed class DocumentExtractionException : Exception() {
    class UnsupportedFormat : DocumentExtractionException()
    class FileAccessFailure : DocumentExtractionException()
    class InvalidFileSize : DocumentExtractionException()
    class FileTooLarge : DocumentExtractionException()
    class ExtractedTextTooLarge : DocumentExtractionException()
    class PdfTextTooLarge : DocumentExtractionException()
    class ZipEntriesLimitExceeded : DocumentExtractionException()
    class ZipContentTooLarge : DocumentExtractionException()
    class ZipTextFileNotFound : DocumentExtractionException()
}
